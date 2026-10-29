package com.everlog.android.common.datasync

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
 * @param fullHistoryMaxAgeMillis How old a SyncPlan.Recent collection's full history may get before an all-time
 * read syncs it again. Never, by default.
 */
class SyncSettings(
    val maxAgeMillis: () -> Long,
    val fullHistoryMaxAgeMillis: () -> Long = { Long.MAX_VALUE },
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
 */
class DataSync(
    private val records: SyncRecords,
    private val settings: SyncSettings,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val stopTimeoutMillis: Long = 5_000,
) {

    private val statuses = MutableStateFlow<Map<String, SyncStatus>>(emptyMap())

    // Each source's sync, by its key, for the Settings sync status
    val status: StateFlow<Map<String, SyncStatus>> = statuses.asStateFlow()

    // Goes up on sign-out, which ends every read started before
    private val sessions = MutableStateFlow(0)

    private val lock = Any()
    private val running = HashMap<String, RunningSync>()
    private val shared = HashMap<String, SharedFlow<Load<*>>>()

    fun <T> observe(source: CollectionSource<T>, range: Range = Range.All): Flow<Load<List<T>>> {
        require(range == Range.All || source.plan is SyncPlan.Recent) { "Range.Last needs a SyncPlan.Recent collection" }
        val fullHistory = needsFullHistory(source.plan, range)
        return observe(source.key, range.toString(), sync = { requestSync(source, range) }) { status ->
            val since = (range as? Range.Last)?.let { windowStart(clock(), it.months) }
            source.backend.cached(since).withSyncStatus(status) { currentStatus(source.key) }.map { view ->
                loadOf(view.cached, view.cached.isEmpty(), covers(view.status, source.plan, range), fullHistory, view.status, view.cacheBehind)
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
            sync = { requestSync(source, Range.Last(plan.months)) },
            fetchPage = { after -> withSyncTimeout(source.name) { source.backend.fetchPage(after, pageSize) } },
            endsWithSession = { it.endsWith(session) },
        )
    }

    // Ready(null) once the server has said the document doesn't exist
    fun <T> observe(source: DocumentSource<T>): Flow<Load<T?>> =
        observe(source.key, "document", sync = { requestSync(source) }) { status ->
            source.backend.cached().withSyncStatus(status) { currentStatus(source.key) }.map { view ->
                loadOf(view.cached, view.cached == null, view.status.lastSynced != null, needsFullHistory = false, view.status, view.cacheBehind)
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
                    .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis), replay = 1)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return (sharedLoads as Flow<Load<T>>).onStart { sync() }.endsWith(session)
    }

    private fun requestSync(source: CollectionSource<*>, range: Range) {
        val work = syncWork(source.plan, range, currentStatus(source.key), clock(), settings.maxAgeMillis(), settings.fullHistoryMaxAgeMillis()) ?: return
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
            SyncWork.FullHistory -> SyncResult(fullHistory(source), fullHistory = true)
            SyncWork.Recent -> {
                val plan = source.plan as SyncPlan.Recent
                val recent = withSyncTimeout(source.name) { backend.fetchSince(windowStart(startedAt, plan.months)) }
                // Older documents only change through a full sync: one missing (evicted, deleted on another
                // phone) or added elsewhere shows as a different count
                if (currentStatus(source.key).fullHistorySynced != null && countsDiffer(source)) {
                    SyncResult(recent + fullHistory(source), fullHistory = true)
                } else {
                    SyncResult(recent)
                }
            }
        }
    }

    // In chunks, so a long history isn't read in one go. Only done once the last chunk has landed.
    private suspend fun fullHistory(source: CollectionSource<*>): Int {
        var documents = 0
        var afterId: String? = null
        do {
            val chunk = withSyncTimeout(source.name) { source.backend.fetchChunk(afterId, settings.chunkSize) }
            documents += chunk.count
            afterId = chunk.lastId
        } while (chunk.count == settings.chunkSize)
        return documents
    }

    // A check that fails doesn't fail the sync: the recent part has landed
    private suspend fun countsDiffer(source: CollectionSource<*>): Boolean = try {
        val server = withSyncTimeout(source.name) { source.backend.countOnServer() }
        val cached = withSyncTimeout(source.name) { source.backend.countInCache() }
        if (server != cached) Timber.tag(TAG).i("%s: %d cached, %d on the server, syncing it all", source.name, cached, server)
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
            if (result.fullHistory) records.setFullHistorySynced(key, startedAt)
            updateStatus(key) {
                it.copy(
                    lastSynced = startedAt,
                    fullHistorySynced = if (result.fullHistory) startedAt else it.fullHistorySynced,
                    syncing = false,
                    syncs = it.syncs + 1,
                    lastFetched = result.documents,
                )
            }
            Timber.tag(TAG).i("Synced %s (%s): %d documents in %d ms", name, work, result.documents, clock() - startedAt)
        } catch (e: CancellationException) {
            // Only sign-out cancels, and it forgets the status
            throw e
        } catch (e: Exception) {
            // Without the throwable: failing offline is expected, not a Crashlytics non-fatal
            Timber.tag(TAG).w("Couldn't sync %s (%s): %s", name, work, e.message)
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

    private fun recordedStatus(key: String) = SyncStatus(records.lastSynced(key), records.fullHistorySynced(key))

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

    private class RunningSync(val work: SyncWork, val job: Job)

    private class SyncResult(val documents: Int, val fullHistory: Boolean = false)

    private class Reading<T>(val value: T)

    private data object Ended
}

private const val TAG = "DataSync"
