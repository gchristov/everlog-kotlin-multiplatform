package com.everlog.android.common.datasync

// Where a source's sync stands, for the Settings sync status and to decide what a screen shows
data class SyncStatus(
    val lastSynced: Long? = null,
    val fullHistorySynced: Long? = null,
    val syncing: Boolean = false,
    val lastFailure: Throwable? = null,
    // Syncs finished since the app started, and how many documents the last one brought
    val syncs: Int = 0,
    val lastFetched: Int = 0,
)
