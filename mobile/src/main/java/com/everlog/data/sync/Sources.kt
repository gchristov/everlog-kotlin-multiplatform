package com.everlog.data.sync

/**
 * A collection a screen reads, and how its cache is kept in step with the server.
 *
 * @param name Names the collection in sync records, the status and logs, e.g. "workouts".
 * @param userId The user the data belongs to, or null for data everyone shares, like the exercise library.
 */
class CollectionSource<T>(
    val name: String,
    val userId: String?,
    val plan: SyncPlan,
    val backend: CollectionBackend<T>,
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
     * The last [months] by [dateField] when due, for big collections (workouts). The rest of the history
     * syncs once, the first time a read needs it: [Range.All], or [Range.Last] longer than [months].
     */
    data class Recent(val dateField: String, val months: Int) : SyncPlan
}

// How much of a collection a screen reads
sealed interface Range {

    data object All : Range

    // By the plan's date field, so only for a SyncPlan.Recent collection
    data class Last(val months: Int) : Range
}

private fun syncKey(name: String, userId: String?) = if (userId == null) name else "$userId/$name"
