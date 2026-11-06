package com.everlog.android.datasync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.CopyOnWriteArrayList

data class Doc(val id: String, val date: Long, val name: String = id)

/**
 * A cache and a server, like Firestore's: syncs copy the server's documents into the cache, and the cache
 * flow shows them. Server calls are recorded and can be held, failed or left hanging.
 */
abstract class FakeServer {

    // Server calls in order, e.g. "all", "since", "between:<from>:<after id>", "count"
    val calls: MutableList<String> = CopyOnWriteArrayList()

    // Server calls wait for it while set
    var gate: CompletableDeferred<Unit>? = null

    // The failure for a server call, by its name
    var fail: (call: String) -> Exception? = { null }

    var hang = false

    // Syncs finish before the cache shows what they brought, as Firestore's can, until delivered
    var cacheLate = false
    private val late = mutableListOf<() -> Unit>()

    fun deliverCache() {
        late.forEach { it() }
        late.clear()
    }

    protected fun land(update: () -> Unit) {
        if (cacheLate) late += update else update()
    }

    protected suspend fun serverCall(call: String) {
        calls += call
        gate?.await()
        if (hang) awaitCancellation()
        fail(call)?.let { throw it }
    }
}

class FakeCollectionBackend(server: List<Doc> = emptyList(), cached: List<Doc> = emptyList()) : FakeServer(), CollectionBackend<Doc> {

    val server = server.toMutableList()
    val cache = MutableStateFlow(cached)

    // Local writes the server hasn't confirmed yet, which syncs don't replace
    private val pending = HashSet<String>()

    override fun cachedDocuments(since: Long?, limit: Int?): Flow<CachedDocuments<Doc>> =
        cache.map { docs ->
            val shown = docs.filter { since == null || it.date >= since }.sortedByDescending { it.date }
            CachedDocuments(shown.take(limit ?: Int.MAX_VALUE), documents = shown.size)
        }

    // Each cachedBetween, as from and until
    val cacheReads: MutableList<Pair<Long, Long?>> = CopyOnWriteArrayList()

    override suspend fun cachedBetween(from: Long, until: Long?): List<Doc> {
        cacheReads += from to until
        return cache.value.filter { it.date >= from && (until == null || it.date < until) }
    }

    override suspend fun fetchPage(after: Int, size: Int): Int {
        serverCall("page:$after")
        val last = cache.value.sortedByDescending { it.date }.take(after).lastOrNull()
        val page = server.sortedByDescending { it.date }.filter { last == null || it.date < last.date }.take(size)
        land { cache.value = cache.value.filterNot { cached -> page.any { it.id == cached.id } } + page }
        return page.size
    }

    override suspend fun fetchAll(): Int {
        serverCall("all")
        val landed = server.filter { it.id !in pending }
        land { cache.value = landed + cache.value.filter { it.id in pending } }
        return server.size
    }

    override suspend fun fetchSince(since: Long): Int {
        serverCall("since")
        val recent = server.filter { it.date >= since && it.id !in pending }
        land { cache.value = cache.value.filter { it.date < since || it.id in pending } + recent }
        return recent.size
    }

    override suspend fun fetchBetween(from: Long, until: Long, after: Cursor?, size: Int): Chunk {
        serverCall("between:$from:${after?.id}")
        val page = server.filter { it.date in from until until }.sortedWith(compareBy({ it.date }, { it.id }))
            .filter { after == null || it.date > after.date || (it.date == after.date && it.id > after.id) }
            .take(size)
        land { cache.value = cache.value.filterNot { cached -> page.any { it.id == cached.id } } + page }
        return Chunk(page.size, page.lastOrNull()?.let { Cursor(it.date, it.id) })
    }

    override suspend fun fetchIds(ids: Collection<String>): Set<String> {
        ids.distinct().chunked(MaxIdsPerRead).forEach { serverCall("ids:${it.joinToString(",")}") }
        val found = server.filter { it.id in ids && it.id !in pending }
        land { cache.value = cache.value.filterNot { cached -> found.any { it.id == cached.id } } + found }
        return found.map { it.id }.toSet()
    }

    override suspend fun oldestOnServer(): Long? {
        serverCall("oldest")
        return server.minOfOrNull { it.date }
    }

    override suspend fun countOnServer(since: Long): Long {
        serverCall("count")
        return server.count { it.date >= since }.toLong()
    }

    // Each countInCache, as from and until
    val cacheCounts: MutableList<Pair<Long, Long?>> = CopyOnWriteArrayList()

    override suspend fun countInCache(from: Long, until: Long?): Long {
        cacheCounts += from to until
        return cache.value.count { it.id !in pending && it.date >= from && (until == null || it.date < until) }.toLong()
    }

    // A local write, which Firestore puts in the cache straight away
    fun write(doc: Doc, uploaded: Boolean = true) {
        if (!uploaded) pending += doc.id
        cache.value = cache.value.filterNot { it.id == doc.id } + doc
    }
}

class FakeDocumentBackend(var server: Doc? = null, cached: Doc? = null) : FakeServer(), DocumentBackend<Doc> {

    val cache = MutableStateFlow(cached)

    override fun cached(): Flow<Doc?> = cache

    override suspend fun fetch(): Boolean {
        serverCall("document")
        val landed = server
        land { cache.value = landed }
        return landed != null
    }
}

class InMemorySyncRecords : SyncRecords {

    val lastSynced = HashMap<String, Long>()
    val syncedBack = HashMap<String, SyncedBack>()

    override fun lastSynced(key: String): Long? = lastSynced[key]

    override fun syncedBack(key: String): SyncedBack? = syncedBack[key]

    override fun setLastSynced(key: String, at: Long) {
        lastSynced[key] = at
    }

    override fun setSyncedBack(key: String, syncedBack: SyncedBack) {
        this.syncedBack[key] = syncedBack
    }

    override fun clear() {
        lastSynced.clear()
        syncedBack.clear()
    }
}
