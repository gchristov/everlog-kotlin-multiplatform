package com.everlog.android.datasync

/**
 * A collection a screen reads, and how its cache is kept in step with the server.
 *
 * @param name Names the collection in sync records, the status and logs, e.g. "workouts".
 * @param userId The user the data belongs to, or null for data everyone shares, like the exercise library.
 * @param completeOnly Shows the shimmer until the first sync has landed in the cache, rather than what's
 * cached before it: for a collection whose documents are also read one at a time, like the exercise
 * library, so the cache may hold only a few of them, and screens that search all of it would miss the rest.
 */
class CollectionSource<T>(
    val name: String,
    val userId: String?,
    val plan: SyncPlan,
    val backend: CollectionBackend<T>,
    val completeOnly: Boolean = false,
) {
    val key: String = syncKey(name, userId)
}

// One document a screen reads, synced whole when due
class DocumentSource<T>(
    val name: String,
    val userId: String?,
    val backend: DocumentBackend<T>,
) {
    val key: String = syncKey(name, userId)
}

sealed interface SyncPlan {

    // The whole collection when due, for small collections: routines, plans, exercises
    data object Whole : SyncPlan

    /**
     * The last [months] by [dateField] when due, for big collections (workouts). A read further back syncs the
     * part it needs the first time, then weekly and whenever the count check finds a difference. There's no
     * reading all of it: each read says how far back it goes.
     */
    data class Recent(val dateField: String, val months: Int) : SyncPlan
}

// How much of a SyncPlan.Recent collection a screen follows, by the plan's date field
sealed interface Range {

    data class Last(val months: Int) : Range
}

private fun syncKey(name: String, userId: String?) = if (userId == null) name else "$userId/$name"
