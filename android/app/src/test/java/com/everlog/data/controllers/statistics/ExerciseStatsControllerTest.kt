package com.everlog.data.controllers.statistics

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.testutil.at
import com.everlog.testutil.loggedSet
import com.everlog.testutil.workout
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment.RangeType
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

    // 1RM estimate

    @Test
    fun `1RM is estimated from the heaviest set`() {
        val exercise = routineExercise(loggedSet(8, 60f), loggedSet(5, 70f), loggedSet(8, 65f))

        // 70 / (1.0278 - 0.0278 * 5)
        assertThat(BaseStatsController.calculate1RM(exercise)).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `1RM ignores sets with more than 10 reps`() {
        // 80kg x 12 would give a 1RM of 115
        val exercise = routineExercise(loggedSet(12, 80f), loggedSet(5, 70f))

        assertThat(BaseStatsController.calculate1RM(exercise)).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `1RM counts sets with exactly 10 reps`() {
        val exercise = routineExercise(loggedSet(10, 80f))

        // 80 / (1.0278 - 0.0278 * 10)
        assertThat(BaseStatsController.calculate1RM(exercise)).isWithin(0.01f).of(106.70f)
    }

    @Test
    fun `1RM is 0 without a set to estimate it from`() {
        val exercise = routineExercise(loggedSet(30, 20f), ELSet(reps = 5), ELSet(weight = 60f))

        assertThat(BaseStatsController.calculate1RM(exercise)).isEqualTo(0f)
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

        assertThat(stats.heaviestSet!!.getWeight()).isEqualTo(80f)
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
