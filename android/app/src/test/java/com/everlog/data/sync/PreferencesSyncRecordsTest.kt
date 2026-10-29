package com.everlog.data.sync

import com.everlog.testutil.InMemorySharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PreferencesSyncRecordsTest {

    private val records = PreferencesSyncRecords(InMemorySharedPreferences())

    @Test
    fun `records are kept per source until cleared`() {
        records.setLastSynced("user/workouts", 10)
        records.setFullHistorySynced("user/workouts", 5)

        assertThat(records.lastSynced("user/workouts")).isEqualTo(10)
        assertThat(records.fullHistorySynced("user/workouts")).isEqualTo(5)
        assertThat(records.lastSynced("user/routines")).isNull()

        records.clear()

        assertThat(records.lastSynced("user/workouts")).isNull()
        assertThat(records.fullHistorySynced("user/workouts")).isNull()
    }
}
