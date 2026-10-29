package com.everlog.android.common.datasync

import android.content.SharedPreferences

// When each source last synced, kept across launches
interface SyncRecords {
    fun lastSynced(key: String): Long?
    fun fullHistorySynced(key: String): Long?
    fun setLastSynced(key: String, at: Long)
    fun setFullHistorySynced(key: String, at: Long)
    fun clear()
}

/**
 * In their own preferences file, which backups leave out (backup_rules.xml): a restored phone has no
 * Firestore cache, so records saying it's synced would hide data.
 */
class PreferencesSyncRecords(private val preferences: SharedPreferences) : SyncRecords {

    override fun lastSynced(key: String): Long? = read(LAST_SYNCED + key)

    override fun fullHistorySynced(key: String): Long? = read(FULL_HISTORY_SYNCED + key)

    override fun setLastSynced(key: String, at: Long) = write(LAST_SYNCED + key, at)

    override fun setFullHistorySynced(key: String, at: Long) = write(FULL_HISTORY_SYNCED + key, at)

    override fun clear() = preferences.edit().clear().apply()

    private fun read(key: String): Long? = if (preferences.contains(key)) preferences.getLong(key, 0) else null

    private fun write(key: String, at: Long) = preferences.edit().putLong(key, at).apply()

    companion object {
        const val FILE = "DataSync"
        private const val LAST_SYNCED = "lastSynced:"
        private const val FULL_HISTORY_SYNCED = "fullHistorySynced:"
    }
}
