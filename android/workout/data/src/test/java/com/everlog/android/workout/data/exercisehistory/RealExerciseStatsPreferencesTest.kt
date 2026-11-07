package com.everlog.android.workout.data.exercisehistory

import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealExerciseStatsPreferencesTest {

    private val preferences = RealExerciseStatsPreferences()
    private lateinit var stored: InMemorySharedPreferences

    @Before
    fun setUp() {
        stored = InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `opens on this month until a range is picked, then on that one for every exercise`() {
        assertThat(preferences.range()).isEqualTo(ExerciseStatsRange.Month)

        preferences.setRange(ExerciseStatsRange.Last12Months)

        assertThat(RealExerciseStatsPreferences().range()).isEqualTo(ExerciseStatsRange.Last12Months)
    }

    @Test
    fun `a range it doesn't know opens this month`() {
        stored.edit().putString("EXERCISE_STATISTICS_RANGE", "Overall").apply()

        assertThat(preferences.range()).isEqualTo(ExerciseStatsRange.Month)
    }
}
