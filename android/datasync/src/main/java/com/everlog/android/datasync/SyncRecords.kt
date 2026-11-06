package com.everlog.android.datasync

import android.content.SharedPreferences

// How far back by the plan's date field a source has been synced beyond its recent window, and when
data class SyncedBack(val to: Long, val at: Long)

// When each source last synced, kept across launches
interface SyncRecords {
    fun lastSynced(key: String): Long?
    fun syncedBack(key: String): SyncedBack?
    fun setLastSynced(key: String, at: Long)
    fun setSyncedBack(key: String, syncedBack: SyncedBack)
    // The oldest document's date by the plan's date field, as the server last said
    fun oldest(key: String): Long?
    fun setOldest(key: String, date: Long)
    fun clear()
}

/**
 * In their own preferences file, which backups leave out (backup_rules.xml): a restored phone has no
 * Firestore cache, so records saying it's synced would hide data.
 */
class PreferencesSyncRecords(private val preferences: SharedPreferences) : SyncRecords {

    override fun lastSynced(key: String): Long? = read(LAST_SYNCED + key)

    override fun syncedBack(key: String): SyncedBack? {
        val to = read(SYNCED_BACK_TO + key) ?: return null
        val at = read(SYNCED_BACK_AT + key) ?: return null
        return SyncedBack(to, at)
    }

    override fun setLastSynced(key: String, at: Long) = write(LAST_SYNCED + key, at)

    override fun setSyncedBack(key: String, syncedBack: SyncedBack) =
        preferences.edit().putLong(SYNCED_BACK_TO + key, syncedBack.to).putLong(SYNCED_BACK_AT + key, syncedBack.at).apply()

    override fun oldest(key: String): Long? = read(OLDEST + key)

    override fun setOldest(key: String, date: Long) = write(OLDEST + key, date)

    override fun clear() = preferences.edit().clear().apply()

    private fun read(key: String): Long? = if (preferences.contains(key)) preferences.getLong(key, 0) else null

    private fun write(key: String, at: Long) = preferences.edit().putLong(key, at).apply()

    companion object {
        const val FILE = "DataSync"
        private const val LAST_SYNCED = "lastSynced:"
        // Older versions' "fullHistorySynced:" records are left unread: those phones sync back again what reads need
        private const val SYNCED_BACK_TO = "syncedBackTo:"
        private const val SYNCED_BACK_AT = "syncedBackAt:"
        private const val OLDEST = "oldest:"
    }
}
