package com.everlog.android.datasync

import com.everlog.android.common.kotlin.Load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * @param olderMaxAgeMillis How old the part of a SyncPlan.Recent collection older than its window may get before a
 * read of it syncs it again. Never, by default.
 * @param chunkSize How many documents a page of the older part brings at a time
 */
class SyncSettings(
    val maxAgeMillis: () -> Long,
    val olderMaxAgeMillis: () -> Long = { Long.MAX_VALUE },
    val timeoutMillis: Long,
    val chunkSize: Int,
)

class SyncTimeoutException(source: String) : Exception("Syncing $source timed out")

/**
 * Screens read through [observe], always from the cache. Reading is what syncs: when the cache is due a
 * refresh, or doesn't reach as far back as the read, a sync starts in [scope], shared with every other
 * read of the same source, and its result arrives through the cache.
 *
 * @param stopTimeoutMillis How long a source's cache listener outlives its last reader, so a rotation
 * doesn't restart it.
 * @param report Told about every sync, finished or failed
 */
class DataSync(
    private val records: SyncRecords,
    private val settings: SyncSettings,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val stopTimeoutMillis: Long = 5_000,
    private val report: (SyncReport) -> Unit = {},
) {

    private val statuses = MutableStateFlow<Map<String, SyncStatus>>(emptyMap())

    // Each source's sync, by its key, for the Settings sync status
    val status: StateFlow<Map<String, SyncStatus>> = statuses.asStateFlow()

    // Goes up on sign-out, which ends every read started before
    private val sessions = MutableStateFlow(0)

    private val lock = Any()
    private val running = HashMap<String, RunningSync>()
    private val shared = HashMap<String, SharedFlow<Load<*>>>()

    // A SyncPlan.Whole collection, all of it, as it changes
    fun <T> observe(source: CollectionSource<T>): Flow<Load<List<T>>> {
        require(source.plan == SyncPlan.Whole) { "A SyncPlan.Recent collection is read by range" }
        return observe(source, since = { null }, variant = "whole", complete = false)
    }

    /**
     * The last months of a SyncPlan.Recent collection, as they change.
     *
     * @param complete Shows nothing until the range has synced, rather than what's cached, as [CollectionSource.completeOnly]
     * does for every read of a source: for a calculation over the range, which a part of it would get wrong.
     */
    fun <T> observe(source: CollectionSource<T>, range: Range, complete: Boolean = false): Flow<Load<List<T>>> {
        require(source.plan is SyncPlan.Recent) { "Only a SyncPlan.Recent collection is read by range" }
        val months = (range as Range.Last).months
        return observe(source, since = { windowStart(clock(), months) }, variant = if (complete) "$range|complete" else range.toString(), complete)
    }

    /**
     * @param since Where the range starts, as of now. Asked again for every sync and every check of whether the cache
     * covers it, as the shared listener outlives the reader that started it: one started before a sync failed, say,
     * would otherwise wait for its own start, which the later sync that succeeds goes back from a little after.
     */
    private fun <T> observe(source: CollectionSource<T>, since: () -> Long?, variant: String, complete: Boolean): Flow<Load<List<T>>> {
        val backTo = { reachesBack(source.plan, since(), clock()) }
        val needsComplete = complete || source.completeOnly || backTo() != null
        return observe(source.key, variant, sync = { requestSync(source, backTo()) }) { status ->
            // The listener's own start: a little earlier than the range's as time goes on, which only shows more
            val listenedSince = since()
            source.backend.cachedDocuments(listenedSince).withSyncStatus(status) { currentStatus(source.key) }.map { view ->
                val cached = view.cached.items
                // Only for the whole collection: a window holds fewer documents than a sync of more of it brings
                if (needsComplete && listenedSince == null && !caughtUp(view.cached.documents, view.status, view.cacheBehind)) {
                    Load.Loading
                } else {
                    loadOf(cached, cached.isEmpty(), covers(view.status, backTo()), needsComplete, view.status, view.cacheBehind)
                }
            }
        }
    }

    /**
     * Newest first, a page at a time, for long lists like the history. Pages come from the cache, which the
     * recent sync fills; past what's cached, before the full history has synced, from the server.
     */
    fun <T> pages(source: CollectionSource<T>, pageSize: Int): PagedRead<T> {
        val plan = requireNotNull(source.plan as? SyncPlan.Recent) { "Paging needs a SyncPlan.Recent collection" }
        val session = sessions.value
        return PagedRead(
            backend = source.backend,
            pageSize = pageSize,
            scope = scope,
            status = statusOf(source.key),
            currentStatus = { currentStatus(source.key) },
            sync = { requestSync(source, backTo = null) },
            fetchPage = { after -> withSyncTimeout(source.name) { source.backend.fetchPage(after, pageSize) } },
            endsWithSession = { it.endsWith(session) },
        )
    }

    /**
     * What's cached from [from] until before [until] by the plan's date field, read once rather than followed:
     * for going through a long history a part at a time, without a listener holding all of it in memory. Like
     * [observe], it syncs when due, and waits for the first sync that reaches back to [from].
     */
    suspend fun <T> read(source: CollectionSource<T>, from: Long, until: Long?): Load<List<T>> {
        require(source.plan is SyncPlan.Recent) { "Reading by date needs a SyncPlan.Recent collection" }
        val backTo = reachesBack(source.plan, from, clock())
        requestSync(source, backTo)
        if (!covers(currentStatus(source.key), backTo)) {
            synchronized(lock) { running[source.key]?.job }?.join()
            val status = currentStatus(source.key)
            if (!covers(status, backTo)) {
                return Load.Unavailable(status.lastFailure ?: IllegalStateException("${source.name} hasn't synced"))
            }
        }
        return Load.Ready(source.backend.cachedBetween(from, until))
    }

    /**
     * The date of a SyncPlan.Recent collection's oldest document by the plan's date field, from the server, as the
     * cache may not reach back that far: where going back through the whole collection a part at a time ends. One
     * server read. Ready with null when the collection is empty. Offline, the date the server last gave, so a history
     * already synced still goes back to its start; Unavailable when it never gave one.
     */
    suspend fun oldest(source: CollectionSource<*>): Load<Long?> {
        require(source.plan is SyncPlan.Recent) { "Only a SyncPlan.Recent collection has a date to go back by" }
        return try {
            val oldest = withSyncTimeout(source.name) { Reading(source.backend.oldestOnServer()) }.value
            oldest?.let { records.setOldest(source.key, it) }
            Load.Ready(oldest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w("Couldn't read %s's oldest date: %s", source.name, e.message)
            records.oldest(source.key)?.let { Load.Ready(it) } ?: Load.Unavailable(e)
        }
    }

    /**
     * Brings the documents with these [ids] from the server into the cache, where readers see them: for documents a
     * read found missing, e.g. a library exercise added since the library last synced, or one of the user's own
     * created on another phone. Ready with the ids the server has, the rest don't exist.
     */
    suspend fun fetchIds(source: CollectionSource<*>, ids: Set<String>): Load<Set<String>> {
        if (ids.isEmpty()) return Load.Ready(emptySet())
        return try {
            Load.Ready(withSyncTimeout(source.name) { source.backend.fetchIds(ids) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w("Couldn't fetch %d of %s: %s", ids.size, source.name, e.message)
            Load.Unavailable(e)
        }
    }

    // Ready(null) once the server has said the document doesn't exist
    fun <T> observe(source: DocumentSource<T>): Flow<Load<T?>> =
        observe(source.key, "document", sync = { requestSync(source) }) { status ->
            source.backend.cached().withSyncStatus(status) { currentStatus(source.key) }.map { view ->
                loadOf(view.cached, view.cached == null, view.status.lastSynced != null, needsComplete = false, view.status, view.cacheBehind)
            }
        }

    // Stops running syncs and forgets every sync, so the next user's reads start from the server
    fun signOut() {
        synchronized(lock) {
            running.values.forEach { it.job.cancel() }
            running.clear()
            shared.clear()
        }
        records.clear()
        statuses.value = emptyMap()
        sessions.update { it + 1 }
    }

    // One cache listener per source and range, however many screens read it. Every reader checks whether a sync is due.
    private fun <T> observe(key: String, variant: String, sync: () -> Unit, loads: (Flow<SyncStatus>) -> Flow<Load<T>>): Flow<Load<T>> {
        val session = sessions.value
        val sharedLoads = synchronized(lock) {
            shared.getOrPut("$key|$variant") {
                loads(statusOf(key))
                    .catch { emit(Load.Unavailable(it)) }
                    .distinctUntilChanged()
                    // Once the listener stops, writes don't reach the replayed copy, so the next reader starts from the cache
                    .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis, replayExpirationMillis = 0), replay = 1)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return (sharedLoads as Flow<Load<T>>).onStart { sync() }.endsWith(session)
    }

    private fun requestSync(source: CollectionSource<*>, backTo: Long?) {
        val work = syncWork(source.plan, backTo, currentStatus(source.key), clock(), settings.maxAgeMillis(), settings.olderMaxAgeMillis()) ?: return
        start(source.key, work) { sync(source, work) }
    }

    private fun requestSync(source: DocumentSource<*>) {
        if (!isDue(currentStatus(source.key).lastSynced, clock(), settings.maxAgeMillis())) return
        start(source.key, SyncWork.Whole) {
            syncing(source.key, source.name, SyncWork.Whole) {
                val exists = withSyncTimeout(source.name) { source.backend.fetch() }
                SyncResult(documents = if (exists) 1 else 0)
            }
        }
    }

    // Joins a running sync that covers the work, otherwise starts it once the running one is done
    private fun start(key: String, work: SyncWork, sync: suspend () -> Unit): Unit = synchronized(lock) {
        val current = running[key]
        if (current != null && current.job.isActive && current.work.covers(work)) return
        val previous = current?.job
        val job = scope.launch(start = CoroutineStart.LAZY) {
            previous?.join()
            sync()
        }
        running[key] = RunningSync(work, job)
        job.invokeOnCompletion { synchronized(lock) { if (running[key]?.job == job) running.remove(key) } }
        job.start()
    }

    private suspend fun sync(source: CollectionSource<*>, work: SyncWork) = syncing(source.key, source.name, work) { startedAt ->
        val backend = source.backend
        when (work) {
            SyncWork.Whole -> SyncResult(withSyncTimeout(source.name) { backend.fetchAll() })
            is SyncWork.Recent -> {
                val plan = source.plan as SyncPlan.Recent
                val windowFrom = windowStart(startedAt, plan.months)
                val recent = withSyncTimeout(source.name) { backend.fetchSince(windowFrom) }
                // The part synced back before only changes here: one missing (evicted, deleted on another phone) or
                // added elsewhere shows as a different count
                val status = currentStatus(source.key)
                val backTo = work.backTo ?: status.syncedBackTo?.takeIf { countsDiffer(source, since = it) }
                val older = backTo?.let { olderSync(it, windowFrom, status, startedAt, settings.olderMaxAgeMillis()) }
                if (older != null) {
                    SyncResult(recent + older(source, older.from, older.until), syncedBack = older.recorded)
                } else {
                    SyncResult(recent)
                }
            }
        }
    }

    // A page at a time, so a long stretch isn't read in one go. Only recorded once the last page has landed.
    private suspend fun older(source: CollectionSource<*>, from: Long, until: Long): Int {
        var documents = 0
        var after: Cursor? = null
        do {
            val chunk = withSyncTimeout(source.name) { source.backend.fetchBetween(from, until, after, settings.chunkSize) }
            documents += chunk.count
            after = chunk.last
        } while (chunk.count == settings.chunkSize)
        return documents
    }

    // A check that fails doesn't fail the sync: the recent part has landed. The cache is counted a year at a time,
    // so it never holds more than that.
    private suspend fun countsDiffer(source: CollectionSource<*>, since: Long): Boolean = try {
        val server = withSyncTimeout(source.name) { source.backend.countOnServer(since) }
        var cached = 0L
        var from = since
        while (from <= clock()) {
            val until = from + CountWindowMillis
            cached += withSyncTimeout(source.name) { source.backend.countInCache(from, until.takeIf { it <= clock() }) }
            from = until
        }
        if (server != cached) Timber.tag(TAG).i("%s: %d cached, %d on the server, syncing it again", source.name, cached, server)
        server != cached
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(TAG).w("Couldn't count %s: %s", source.name, e.message)
        false
    }

    private suspend fun syncing(key: String, name: String, work: SyncWork, sync: suspend (startedAt: Long) -> SyncResult) {
        val startedAt = clock()
        updateStatus(key) { it.copy(syncing = true, lastFailure = null) }
        try {
            val result = sync(startedAt)
            records.setLastSynced(key, startedAt)
            result.syncedBack?.let { records.setSyncedBack(key, it) }
            updateStatus(key) {
                it.copy(
                    lastSynced = startedAt,
                    syncedBackTo = result.syncedBack?.to ?: it.syncedBackTo,
                    syncedBackAt = result.syncedBack?.at ?: it.syncedBackAt,
                    syncing = false,
                    syncs = it.syncs + 1,
                    lastFetched = result.documents,
                )
            }
            Timber.tag(TAG).i("Synced %s (%s): %d documents in %d ms", name, work, result.documents, clock() - startedAt)
            report(SyncReport(name, work.reportName(result.syncedBack != null), result.documents, clock() - startedAt, failure = null))
        } catch (e: CancellationException) {
            // Only sign-out cancels, and it forgets the status
            throw e
        } catch (e: Exception) {
            // Without the throwable: failing offline is expected, not a Crashlytics non-fatal
            Timber.tag(TAG).w("Couldn't sync %s (%s): %s", name, work, e.message)
            report(SyncReport(name, work.reportName(syncedBack = false), documents = 0, clock() - startedAt, failure = e))
            updateStatus(key) { it.copy(syncing = false, lastFailure = e) }
        }
    }

    private suspend fun <R : Any> withSyncTimeout(name: String, read: suspend () -> R): R =
        withTimeoutOrNull(settings.timeoutMillis) { read() } ?: throw SyncTimeoutException(name)

    private fun statusOf(key: String): Flow<SyncStatus> =
        statuses.map { it[key] ?: recordedStatus(key) }.distinctUntilChanged()

    private fun currentStatus(key: String): SyncStatus = statuses.value[key] ?: recordedStatus(key)

    private fun updateStatus(key: String, change: (SyncStatus) -> SyncStatus) =
        statuses.update { it + (key to change(it[key] ?: recordedStatus(key))) }

    private fun recordedStatus(key: String): SyncStatus {
        val syncedBack = records.syncedBack(key)
        return SyncStatus(records.lastSynced(key), syncedBackTo = syncedBack?.to, syncedBackAt = syncedBack?.at)
    }

    private fun <T> Flow<T>.endsWith(session: Int): Flow<T> {
        val ended = sessions.filter { it != session }.map { Ended }
        return merge(map { Reading(it) }, ended).transformWhile { event ->
            if (event is Reading<*>) {
                @Suppress("UNCHECKED_CAST")
                emit(event.value as T)
            }
            event is Reading<*>
        }
    }

    private fun SyncWork.reportName(syncedBack: Boolean) = when {
        this == SyncWork.Whole -> "whole"
        this is SyncWork.Recent && backTo != null -> "back"
        syncedBack -> "recount"
        else -> "recent"
    }

    private class RunningSync(val work: SyncWork, val job: Job)

    // [syncedBack] when it synced back beyond the window
    private class SyncResult(val documents: Int, val syncedBack: SyncedBack? = null)

    private class Reading<T>(val value: T)

    private data object Ended
}

private const val TAG = "DataSync"

private val CountWindowMillis = java.util.concurrent.TimeUnit.DAYS.toMillis(365)
