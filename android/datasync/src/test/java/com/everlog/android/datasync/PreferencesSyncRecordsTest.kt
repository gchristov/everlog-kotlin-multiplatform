package com.everlog.android.datasync

import com.everlog.android.common.test.InMemorySharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PreferencesSyncRecordsTest {

    private val preferences = InMemorySharedPreferences()
    private val records = PreferencesSyncRecords(preferences)

    @Test
    fun `records are kept per source until cleared`() {
        records.setLastSynced("user/workouts", 10)
        records.setSyncedBack("user/workouts", SyncedBack(to = 2, at = 5))
        records.setOldest("user/workouts", 1)

        assertThat(records.lastSynced("user/workouts")).isEqualTo(10)
        assertThat(records.syncedBack("user/workouts")).isEqualTo(SyncedBack(to = 2, at = 5))
        assertThat(records.oldest("user/workouts")).isEqualTo(1)
        assertThat(records.oldest("user/routines")).isNull()
        assertThat(records.lastSynced("user/routines")).isNull()
        assertThat(records.syncedBack("user/routines")).isNull()

        records.clear()

        assertThat(records.lastSynced("user/workouts")).isNull()
        assertThat(records.syncedBack("user/workouts")).isNull()
        assertThat(records.oldest("user/workouts")).isNull()
    }

    @Test
    fun `an older version's full history record isn't read as synced back`() {
        preferences.edit().putLong("fullHistorySynced:user/workouts", 5).apply()

        assertThat(records.syncedBack("user/workouts")).isNull()
    }
}
