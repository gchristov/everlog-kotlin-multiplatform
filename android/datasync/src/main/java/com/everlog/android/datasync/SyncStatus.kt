package com.everlog.android.datasync

/**
 * One sync, finished or failed, for diagnostics.
 *
 * @param work What it synced: "whole", "recent", "back" (further back than the window, for a read) or "recount"
 * (the part synced back, again, as its count differed)
 */
data class SyncReport(val source: String, val work: String, val documents: Int, val durationMillis: Long, val failure: Throwable?)

// Where a source's sync stands, for the Settings sync status and to decide what a screen shows
data class SyncStatus(
    val lastSynced: Long? = null,
    // How far back by the plan's date field the cache has been synced beyond the recent window, and when
    val syncedBackTo: Long? = null,
    val syncedBackAt: Long? = null,
    val syncing: Boolean = false,
    val lastFailure: Throwable? = null,
    // Syncs finished since the app started, and how many documents the last one brought
    val syncs: Int = 0,
    val lastFetched: Int = 0,
)
