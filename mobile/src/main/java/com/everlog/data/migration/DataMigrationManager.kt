package com.everlog.data.migration

import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.managers.preferences.PreferencesManager
import timber.log.Timber

class DataMigrationManager : PreferencesManager() {

    private val TAG = "DataMigrationManager"

    private enum class PreferenceKeys {
        SCHEMA_VERSION
    }

    private val mMigrationsMap = HashMap<Int, Migration>()

    companion object {

        private const val CURRENT_SCHEMA_VERSION = 1

        @JvmField
        val manager = DataMigrationManager()
    }

    init {
        buildMigrationsMap()
    }

    fun checkMigrations(): Boolean {
        if (needsMigration()) {
            migrateToLatestVersion()
            return true
        }
        return false
    }

    private fun migrateToLatestVersion() {
        Timber.tag(TAG).d("Starting migrations")
        try {
            var migrationVersion = localSchemaVersion()
            while (migrationVersion < CURRENT_SCHEMA_VERSION) {
                migrationVersion++
                val migration = mMigrationsMap[migrationVersion]
                migration?.migrate(FirestorePathManager)
            }
            savePreference(CURRENT_SCHEMA_VERSION, PreferenceKeys.SCHEMA_VERSION.name)
            Timber.tag(TAG).d("Finished migrations")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e)
            Timber.tag(TAG).d("Failed migrations")
        }
    }

    private fun needsMigration(): Boolean {
        return localSchemaVersion() != CURRENT_SCHEMA_VERSION
    }

    private fun localSchemaVersion(): Int {
        return getPreference(PreferenceKeys.SCHEMA_VERSION.name, 0)
    }

    private fun buildMigrationsMap() {
        mMigrationsMap[1] = Migration1()
    }
}