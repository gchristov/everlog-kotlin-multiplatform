package com.everlog.data.repositories

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

// Cache-first reads, like the stores do (Source.CACHE first, then live). A plain get() waits for the
// server whenever the device thinks it's online, so on a weak signal (a gym's basement) a screen sat
// loading for seconds before falling back to the cache, and every read paid for every document.

/**
 * The cached results when there are any, otherwise the server's (or the cache's when offline).
 *
 * @param emptyIsAnswer An empty cache is the answer too, for data a live listener keeps current
 * (the user's own exercises, their routines), which is often genuinely empty.
 * @param refreshKey When given, a cached answer also asks the server for fresh data once per app
 * session, in the background, without waiting for it: the next read gets it from the cache. For data
 * that rarely changes and nothing else keeps fresh, like the exercise library and its registry.
 */
internal suspend fun Query.getCacheFirst(emptyIsAnswer: Boolean = false, refreshKey: String? = null): QuerySnapshot {
    val cached = cachedOrNull { get(Source.CACHE) }
    if (cached == null || (cached.isEmpty && !emptyIsAnswer)) return get().await()
    refreshOncePerSession(refreshKey) { get(Source.SERVER) }
    return cached
}

// The same for one document
internal suspend fun DocumentReference.getCacheFirst(refreshKey: String? = null): DocumentSnapshot {
    val cached = cachedOrNull { get(Source.CACHE) }
    if (cached == null || !cached.exists()) return get().await()
    refreshOncePerSession(refreshKey) { get(Source.SERVER) }
    return cached
}

// A cache read fails when there's nothing cached, e.g. a document never read; cancellation still cancels
private suspend fun <T> cachedOrNull(read: () -> Task<T>): T? = try {
    read().await()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

// Fire and forget: Firestore updates its cache when it answers. A refresh that fails (e.g. offline)
// doesn't count, so the next read tries again.
private fun refreshOncePerSession(key: String?, refresh: () -> Task<*>) {
    if (key == null || !SessionRefreshes.add(key)) return
    refresh().addOnFailureListener { SessionRefreshes.remove(key) }
}

// What's been refreshed from the server since the app started
private val SessionRefreshes: MutableSet<String> = ConcurrentHashMap.newKeySet()
