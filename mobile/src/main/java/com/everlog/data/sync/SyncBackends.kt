package com.everlog.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Where a collection's data lives: Firestore's cache, kept in step with the server. [DataSync] decides
 * when to sync; a backend only does what it's asked. Tests fake it.
 */
interface CollectionBackend<T> {

    /**
     * What's cached, in the order screens show it, as it changes: local writes straight away, a sync once
     * it lands. Only from [since] by the plan's date field when given. Documents that don't parse are left out.
     */
    fun cached(since: Long?): Flow<List<T>>

    // The whole collection from the server into the cache. Returns how many documents it read.
    suspend fun fetchAll(): Int

    // The documents from [since] by the plan's date field. Returns how many it read.
    suspend fun fetchSince(since: Long): Int

    // One chunk of the whole collection by document id, after [afterId]
    suspend fun fetchChunk(afterId: String?, size: Int): Chunk

    suspend fun countOnServer(): Long

    // Documents the server has confirmed, so edits still waiting to upload don't count
    suspend fun countInCache(): Long
}

data class Chunk(val count: Int, val lastId: String?)

interface DocumentBackend<T> {

    // The cached document as it changes, or null when it isn't cached, doesn't exist or doesn't parse
    fun cached(): Flow<T?>

    // The document from the server into the cache, including that it doesn't exist
    suspend fun fetch()
}
