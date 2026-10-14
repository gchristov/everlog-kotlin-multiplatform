package com.everlog.data.repositories

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

// Cache-first reads, like the stores do (Source.CACHE first, then live). A plain get() waits for the
// server whenever the device thinks it's online, so on a weak signal (a gym's basement) a screen sat
// loading for seconds before falling back to the cache, and every read paid for every document.

/**
 * The cached results when there are any, otherwise the server's (or the cache's when offline).
 *
 * @param refreshKey When given, a cached answer also asks the server for fresh data once per app
 * session, in the background, without waiting for it: the next read gets it from the cache. For data
 * that rarely changes and nothing else keeps fresh, like the exercise library and its registry.
 */
internal suspend fun Query.getCacheFirst(refreshKey: String? = null): QuerySnapshot {
    val cached = runCatching { get(Source.CACHE).await() }.getOrNull()
    if (cached == null || cached.isEmpty) return get().await()
    if (refreshKey != null && SessionRefreshes.add(refreshKey)) get(Source.SERVER)
    return cached
}

// The same for one document
internal suspend fun DocumentReference.getCacheFirst(refreshKey: String? = null): DocumentSnapshot {
    val cached = runCatching { get(Source.CACHE).await() }.getOrNull()
    if (cached == null || !cached.exists()) return get().await()
    if (refreshKey != null && SessionRefreshes.add(refreshKey)) get(Source.SERVER)
    return cached
}

// What's been refreshed from the server since the app started
private val SessionRefreshes: MutableSet<String> = ConcurrentHashMap.newKeySet()
