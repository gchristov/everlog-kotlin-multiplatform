package com.everlog.data.controllers.statistics

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.datatest.at
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.testfixtures.install
import com.everlog.android.settings.testfixtures.uninstall

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExerciseStatsControllerTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    // Exercise stats

    @Test
    fun `1RM chart leaves out workouts with only high-rep sets`() {
        val history = listOf(
                workout(at(2026, 9, 20), bench to listOf(loggedSet(12, 80f), loggedSet(5, 70f))),
                // 20kg x 30 would give a 1RM of 103
                workout(at(2025, 6, 1), bench to listOf(loggedSet(30, 20f))))

        val stats = calculateStats(history)

        assertThat(stats.ormCounts).hasSize(1)
        assertThat(stats.ormCounts.single().y).isWithin(0.01f).of(78.76f)
        assertThat(stats.orm).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `heaviest set still includes high-rep sets`() {
        val history = listOf(workout(at(2026, 9, 20), bench to listOf(loggedSet(12, 80f), loggedSet(5, 70f))))

        val stats = calculateStats(history)

        assertThat(stats.heaviestSet!!.getWeight(SettingsManager.manager.weightUnit())).isEqualTo(80f)
        assertThat(stats.weightCounts.single().y).isEqualTo(80f)
    }

    // Helpers

    private fun routineExercise(vararg sets: ELSet) = ELRoutineExercise("re", bench, sets.toMutableList())

    private fun calculateStats(history: List<ELWorkout>): ExerciseStatsController.StatsResult {
        // OVERALL so the result doesn't depend on today's date
        return ExerciseStatsController().calculateStats(RangeType.OVERALL, listOf(bench), history, false)
                .toBlocking().first().getValue(bench.uuid!!)
    }
}
