package com.everlog.android.common.datasync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * What's cached with the sync's status, as either changes.
 *
 * @param cacheBehind The cache hasn't shown anything since the last sync finished.
 */
internal data class CacheView<T>(val cached: T, val status: SyncStatus, val cacheBehind: Boolean)

/**
 * Like combine, but also knows whether the cache has caught up with the last sync. A cache emission
 * shows every sync finished before it; one that arrives before any status counts the syncs [currentStatus]
 * has then, so a new listener isn't behind syncs it already shows.
 */
internal fun <T> Flow<T>.withSyncStatus(status: Flow<SyncStatus>, currentStatus: () -> SyncStatus): Flow<CacheView<T>> = flow {
    var cached: Cached<T>? = null
    var latest: SyncStatus? = null
    // The syncs finished when the cache last showed something
    var cacheSeenAfter = 0
    merge(map { Cached(it) }, status).collect { event ->
        if (event is Cached<*>) {
            @Suppress("UNCHECKED_CAST")
            cached = event as Cached<T>
            cacheSeenAfter = (latest ?: currentStatus()).syncs
        } else {
            latest = event as SyncStatus
        }
        val current = latest ?: return@collect
        val shown = cached ?: return@collect
        emit(CacheView(shown.value, current, cacheBehind = cacheSeenAfter < current.syncs))
    }
}

private class Cached<T>(val value: T)
