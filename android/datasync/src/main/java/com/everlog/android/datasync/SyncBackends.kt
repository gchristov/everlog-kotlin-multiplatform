package com.everlog.android.datasync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Where a collection's data lives: Firestore's cache, kept in step with the server. [DataSync] decides
 * when to sync; a backend only does what it's asked. Tests fake it.
 */
interface CollectionBackend<T> {

    /**
     * What's cached, in the order screens show it, as it changes: local writes straight away, a sync once
     * it lands. Only from [since] by the plan's date field, and the first [limit], when given. Documents
     * that don't parse are left out.
     */
    fun cached(since: Long?, limit: Int? = null): Flow<List<T>> = cachedDocuments(since, limit).map { it.items }

    // The same, with how many documents that is, parsed or not
    fun cachedDocuments(since: Long?, limit: Int? = null): Flow<CachedDocuments<T>>

    /**
     * What's cached from [from] until before [until] by the plan's date field, once, in no particular order.
     * Unlike a listener, it keeps nothing once read, so a long history can be gone through a part at a time.
     */
    suspend fun cachedBetween(from: Long, until: Long?): List<T>

    // The whole collection from the server into the cache. Returns how many documents it read.
    suspend fun fetchAll(): Int

    // The documents from [since] by the plan's date field. Returns how many it read.
    suspend fun fetchSince(since: Long): Int

    // The [size] documents from the server that follow the first [after] cached ones, in the order screens show them
    suspend fun fetchPage(after: Int, size: Int): Int

    /**
     * The next [size] documents from [from] until before [until] by the plan's date field, oldest first, after
     * [after], from the server into the cache.
     */
    suspend fun fetchBetween(from: Long, until: Long, after: Cursor?, size: Int): Chunk

    /**
     * The documents with these [ids] from the server into the cache, at most [MaxIdsPerRead] at a time as Firestore
     * takes in one read. Returns the ids the server has.
     */
    suspend fun fetchIds(ids: Collection<String>): Set<String>

    // The plan's date field of the oldest document on the server, or null when there are none
    suspend fun oldestOnServer(): Long?

    // From [since] by the plan's date field
    suspend fun countOnServer(since: Long): Long

    // From [from] until before [until] by the plan's date field. Documents the server has confirmed, so edits still
    // waiting to upload don't count.
    suspend fun countInCache(from: Long, until: Long?): Long
}

// How many ids Firestore takes in one `in` query
const val MaxIdsPerRead = 30

// Where a page of documents ordered by date ended: the last one's date and id
data class Cursor(val date: Long, val id: String)

data class Chunk(val count: Int, val last: Cursor?)

data class CachedDocuments<T>(val items: List<T>, val documents: Int)

interface DocumentBackend<T> {

    // The cached document as it changes, or null when it isn't cached, doesn't exist or doesn't parse
    fun cached(): Flow<T?>

    // The document from the server into the cache, including that it doesn't exist. Returns whether it does.
    suspend fun fetch(): Boolean
}
