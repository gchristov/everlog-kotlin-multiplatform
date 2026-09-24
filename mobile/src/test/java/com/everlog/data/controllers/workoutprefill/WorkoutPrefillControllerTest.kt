package com.everlog.data.controllers.workoutprefill

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.testutil.at
import com.everlog.testutil.loggedSet
import com.everlog.testutil.plannedSet
import com.everlog.testutil.workout
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutPrefillControllerTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val squat = ELExercise(uuid = "squat", name = "Squat")

    // The first day of a month, which is when prefilling used to reset (TAS-409)
    private val now = at(2026, 10, 1)

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
        SettingsManager.manager.setWeightUnit(WeightUnit.KILOGRAM)
        SettingsManager.manager.setMuscleGoal(MuscleGoal.HISTORY)
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    // History

    @Test
    fun `history prefills from a session in the previous month`() {
        val history = listOf(workout(at(2026, 9, 29), bench to listOf(loggedSet(8, 60f), loggedSet(8, 62.5f))))
        val ongoing = workout(now, bench to listOf(plannedSet(), plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(60f, 62.5f).inOrder()
    }

    @Test
    fun `history prefills from a session several months ago`() {
        val history = listOf(workout(at(2026, 3, 14), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(60f)
    }

    @Test
    fun `history uses the most recently completed session regardless of store order`() {
        // The store orders by created date, so an older workout can come first
        val history = listOf(
                workout(at(2026, 9, 10), bench to listOf(loggedSet(8, 55f))),
                workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 65f))),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(65f)
    }

    @Test
    fun `history skips sessions where the exercise has no logged sets`() {
        val history = listOf(
                workout(at(2026, 9, 28), bench to listOf(plannedSet())),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(60f)
    }

    @Test
    fun `history matches sets by position and leaves extra sets empty`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f), loggedSet(6, 70f))))
        val ongoing = workout(now, bench to listOf(plannedSet(), plannedSet(), plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(60f, 70f, -1f).inOrder()
    }

    @Test
    fun `history only uses sessions of the same exercise`() {
        val history = listOf(
                workout(at(2026, 9, 28), squat to listOf(loggedSet(5, 100f))),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()), squat to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(60f)
        assertThat(weights(ongoing, squat)).containsExactly(100f)
    }

    @Test
    fun `does not overwrite a weight that's already entered`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(loggedSet(8, 40f)))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(40f)
    }

    @Test
    fun `leaves sets empty when the exercise has no history`() {
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, emptyList(), now)

        assertThat(weights(ongoing, bench)).containsExactly(-1f)
    }

    @Test
    fun `history prefill keeps the weight when using pounds`() {
        SettingsManager.manager.setWeightUnit(WeightUnit.POUND)
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        // 60kg in pounds
        assertThat(weights(ongoing, bench).single()).isWithin(0.01f).of(132.28f)
    }

    // 1RM

    @Test
    fun `1RM goal targets a percentage of a 1RM from the previous month`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        // Brzycki: 90 / (1.0278 - 0.0278 * 5) = 101.26
        val history = listOf(workout(at(2026, 9, 29), bench to listOf(loggedSet(5, 90f))))
        val ongoing = workout(now, bench to listOf(plannedSet(), plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        // 80% of 101.26 = 81.01, rounded up
        assertThat(weights(ongoing, bench)).containsExactly(82f, 82f)
    }

    @Test
    fun `1RM goal prefers a recent 1RM over an older, higher one`() {
        val history = listOf(
                workout(at(2026, 8, 1), bench to listOf(loggedSet(5, 90f))),
                workout(at(2025, 1, 1), bench to listOf(loggedSet(5, 120f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)

        assertThat(source.orm).isWithin(0.01f).of(101.26f)
    }

    @Test
    fun `1RM goal falls back to the all-time 1RM when there's nothing recent`() {
        val history = listOf(
                workout(at(2026, 3, 1), bench to listOf(loggedSet(5, 90f))),
                workout(at(2025, 1, 1), bench to listOf(loggedSet(5, 120f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)

        // 120 / (1.0278 - 0.0278 * 5)
        assertThat(source.orm).isWithin(0.01f).of(135.01f)
    }

    @Test
    fun `1RM goal leaves sets empty when no set has weight`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(ELSet(reps = 8))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(-1f)
    }

    @Test
    fun `1RM target isn't bumped up by float noise`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        // A single rep's 1RM is its weight, which comes out as 100.00001
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(1, 100f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(80f)
    }

    @Test
    fun `1RM ignores sets with more than 10 reps`() {
        // 20kg x 30 would give a 1RM of 103, and 80kg x 12 one of 115
        val history = listOf(
                workout(at(2026, 9, 28), bench to listOf(loggedSet(30, 20f))),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(12, 80f), loggedSet(5, 70f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)

        // 70 / (1.0278 - 0.0278 * 5)
        assertThat(source.orm).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `1RM goal falls back to history when every weighted set has more than 10 reps`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(30, 20f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        WorkoutPrefillController.prefill(ongoing, history, now)

        assertThat(weights(ongoing, bench)).containsExactly(20f)
    }

    // Helpers

    private fun weights(workout: ELWorkout, exercise: ELExercise): List<Float> {
        return workout.findExercise(exercise)!!.single().sets.map { it.getWeight() }
    }
}
