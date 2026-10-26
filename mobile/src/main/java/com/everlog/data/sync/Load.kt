package com.everlog.data.sync

/**
 * What a screen shows for data it reads from the cache. There's no state for "syncing": a screen shows
 * what's cached, and data from a sync arrives as a new [Ready].
 */
sealed interface Load<out T> {

    // Nothing cached and never synced: the full-screen shimmer while the server is asked
    data object Loading : Load<Nothing>

    // Empty only once the server has said so, e.g. no routines yet
    data class Ready<T>(val value: T) : Load<T>

    // Nothing cached, never synced, and the server can't be reached. Collecting again tries again.
    data class Unavailable(val error: Throwable) : Load<Nothing>
}
