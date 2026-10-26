package com.everlog.data.sync

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

    // Server calls in order, e.g. "all", "since", "chunk:a", "count"
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

    override fun cached(since: Long?, limit: Int?): Flow<List<Doc>> =
        cache.map { docs -> docs.filter { since == null || it.date >= since }.sortedByDescending { it.date }.take(limit ?: Int.MAX_VALUE) }

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

    override suspend fun fetchChunk(afterId: String?, size: Int): Chunk {
        serverCall("chunk:$afterId")
        val chunk = server.sortedBy { it.id }.filter { afterId == null || it.id > afterId }.take(size)
        land { cache.value = cache.value.filterNot { cached -> chunk.any { it.id == cached.id } } + chunk }
        return Chunk(chunk.size, chunk.lastOrNull()?.id)
    }

    override suspend fun countOnServer(): Long {
        serverCall("count")
        return server.size.toLong()
    }

    override suspend fun countInCache(): Long = cache.value.count { it.id !in pending }.toLong()

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
    val fullHistorySynced = HashMap<String, Long>()

    override fun lastSynced(key: String): Long? = lastSynced[key]

    override fun fullHistorySynced(key: String): Long? = fullHistorySynced[key]

    override fun setLastSynced(key: String, at: Long) {
        lastSynced[key] = at
    }

    override fun setFullHistorySynced(key: String, at: Long) {
        fullHistorySynced[key] = at
    }

    override fun clear() {
        lastSynced.clear()
        fullHistorySynced.clear()
    }
}
