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

fun <T, R> Load<T>.map(transform: (T) -> R): Load<R> = when (this) {
    is Load.Ready -> Load.Ready(transform(value))
    Load.Loading -> Load.Loading
    is Load.Unavailable -> this
}

/**
 * Joins two loads: both values once both are ready, unavailable as soon as either is, and loading otherwise.
 */
fun <A, B, R> Load<A>.zip(other: Load<B>, transform: (A, B) -> R): Load<R> = when (this) {
    is Load.Unavailable -> this
    Load.Loading -> if (other is Load.Unavailable) other else Load.Loading
    is Load.Ready -> other.map { transform(value, it) }
}

/**
 * For a part a join can do without: null once it's answered without data, so it neither holds the join up nor fails it.
 */
fun <T> Load<T>.optional(): Load<T?> = if (this is Load.Unavailable) Load.Ready(null) else this
