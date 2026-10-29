package com.everlog.data.repositories

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

// Cache-first reads, like the stores do (Source.CACHE first, then live). A plain get() waits for the
// server whenever the device thinks it's online, so on a weak signal (a gym's basement) a screen sat
// loading for seconds before falling back to the cache, and every read paid for every document.
// New reads go through DataSync (data/sync) instead.

/**
 * The cached results when there are any, otherwise the server's (or the cache's when offline).
 *
 * @param emptyIsAnswer An empty cache is the answer too, for data a live listener keeps current
 * (the user's routines, their workouts), which is often genuinely empty.
 */
internal suspend fun Query.getCacheFirst(emptyIsAnswer: Boolean = false): QuerySnapshot {
    val cached = cachedOrNull { get(Source.CACHE) }
    if (cached == null || (cached.isEmpty && !emptyIsAnswer)) return get().await()
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
