package com.everlog.android.exercise.details.statistics

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.datatest.at
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.DayOfWeek

class ExerciseStatsControllerTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")

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

        assertThat(stats.heaviestSet!!.getWeight(WeightUnit.KILOGRAM)).isEqualTo(80f)
        assertThat(stats.weightCounts.single().y).isEqualTo(80f)
    }

    // Helpers

    private fun routineExercise(vararg sets: ELSet) = ELRoutineExercise("re", bench, sets.toMutableList())

    private fun calculateStats(history: List<ELWorkout>): ExerciseStatsController.StatsResult {
        // OVERALL so the result doesn't depend on today's date
        return ExerciseStatsController(WeightUnit.KILOGRAM, DayOfWeek.MONDAY).calculateStats(RangeType.OVERALL, listOf(bench), history, false)
                .toBlocking().first().getValue(bench.uuid!!)
    }
}
