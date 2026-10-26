package com.everlog.data.sync

// Where a source's sync stands, for the Settings sync status and to decide what a screen shows
data class SyncStatus(
    val lastSynced: Long? = null,
    val fullHistorySynced: Long? = null,
    val syncing: Boolean = false,
    val lastFailure: Throwable? = null,
)
