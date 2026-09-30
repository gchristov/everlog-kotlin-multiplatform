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
import java.util.concurrent.TimeUnit

class WorkoutPrefillControllerTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val squat = ELExercise(uuid = "squat", name = "Squat")
    private val plank = ELExercise(uuid = "plank", name = "Plank")

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

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(60f, 62.5f).inOrder()
    }

    @Test
    fun `history prefills from a session several months ago`() {
        val history = listOf(workout(at(2026, 3, 14), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, history)

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

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(65f)
    }

    @Test
    fun `history skips sessions where the exercise has no logged sets`() {
        val history = listOf(
                workout(at(2026, 9, 28), bench to listOf(plannedSet())),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(60f)
    }

    @Test
    fun `history matches sets by position and leaves extra sets empty`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f), loggedSet(6, 70f))))
        val ongoing = workout(now, bench to listOf(plannedSet(), plannedSet(), plannedSet()))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(60f, 70f, -1f).inOrder()
    }

    @Test
    fun `history only uses sessions of the same exercise`() {
        val history = listOf(
                workout(at(2026, 9, 28), squat to listOf(loggedSet(5, 100f))),
                workout(at(2026, 9, 20), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()), squat to listOf(plannedSet()))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(60f)
        assertThat(weights(ongoing, squat)).containsExactly(100f)
    }

    @Test
    fun `does not overwrite a weight that's already entered`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(loggedSet(8, 40f)))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(40f)
    }

    @Test
    fun `leaves sets empty when the exercise has no history`() {
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, emptyList())

        assertThat(weights(ongoing, bench)).containsExactly(-1f)
    }

    @Test
    fun `history prefill keeps the weight when using pounds`() {
        SettingsManager.manager.setWeightUnit(WeightUnit.POUND)
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(8, 60f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, history)

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

        prefill(ongoing, history)

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
    fun `1RM goal falls back to the last session's weights when there's no recent 1RM`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        // Over 90 days ago, e.g. coming back from a break
        val history = listOf(
                workout(at(2026, 6, 1), bench to listOf(loggedSet(5, 90f), loggedSet(5, 85f))),
                workout(at(2025, 1, 1), bench to listOf(loggedSet(5, 120f))))
        val ongoing = workout(now, bench to listOf(plannedSet(), plannedSet()))

        prefill(ongoing, history)

        assertThat(WorkoutPrefillController.buildPrefillSource(bench, history, now).orm).isEqualTo(0f)
        assertThat(weights(ongoing, bench)).containsExactly(90f, 85f).inOrder()
    }

    @Test
    fun `1RM counts a session exactly 90 days ago`() {
        val history = listOf(workout(now - TimeUnit.DAYS.toMillis(90), bench to listOf(loggedSet(5, 90f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)

        assertThat(source.orm).isWithin(0.01f).of(101.26f)
    }

    @Test
    fun `1RM goal leaves sets empty when no set has weight`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(ELSet(reps = 8))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(-1f)
    }

    @Test
    fun `1RM target isn't bumped up by float noise`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        // A single rep's 1RM is its weight, which comes out as 100.00001
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(1, 100f))))
        val ongoing = workout(now, bench to listOf(plannedSet()))

        prefill(ongoing, history)

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

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(20f)
    }

    // Reps and time

    @Test
    fun `history prefills reps by set position and leaves extra sets empty`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), loggedSet(8, 65f))))
        val ongoing = workout(now, bench to listOf(ELSet(), ELSet(), ELSet()))

        prefill(ongoing, history)

        assertThat(reps(ongoing, bench)).containsExactly(10, 8, -1).inOrder()
    }

    @Test
    fun `does not overwrite reps that are already entered`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        val ongoing = workout(now, bench to listOf(ELSet(reps = 12)))

        prefill(ongoing, history)

        assertThat(reps(ongoing, bench)).containsExactly(12)
    }

    @Test
    fun `keeps a template's reps and still prefills the weight`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        // As started from a template, see ELWorkout.prefillRequiredMetrics
        val ongoing = workout(now, bench to listOf(ELSet(requiredReps = 5, reps = 5)))

        prefill(ongoing, history)

        assertThat(reps(ongoing, bench)).containsExactly(5)
        assertThat(weights(ongoing, bench)).containsExactly(60f)
    }

    @Test
    fun `history prefills time for timed sets by set position`() {
        val history = listOf(workout(at(2026, 9, 28), plank to listOf(ELSet(timeSeconds = 45), ELSet(timeSeconds = 30))))
        val ongoing = workout(now, plank to listOf(ELSet(), ELSet()))

        prefill(ongoing, history)

        assertThat(times(ongoing, plank)).containsExactly(45, 30).inOrder()
        assertThat(reps(ongoing, plank)).containsExactly(-1, -1)
    }

    @Test
    fun `does not give reps to a timed set`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        val ongoing = workout(now, bench to listOf(ELSet(timeSeconds = 30)))

        prefill(ongoing, history)

        assertThat(times(ongoing, bench)).containsExactly(30)
        assertThat(reps(ongoing, bench)).containsExactly(-1)
    }

    @Test
    fun `does not give a time to a set with reps`() {
        val history = listOf(workout(at(2026, 9, 28), plank to listOf(ELSet(timeSeconds = 45))))
        val ongoing = workout(now, plank to listOf(ELSet(reps = 10)))

        prefill(ongoing, history)

        assertThat(reps(ongoing, plank)).containsExactly(10)
        assertThat(times(ongoing, plank)).containsExactly(0)
    }

    @Test
    fun `does not replace a template's time target with reps`() {
        val history = listOf(workout(at(2026, 9, 28), plank to listOf(loggedSet(10, 20f))))
        val ongoing = workout(now, plank to listOf(ELSet(requiredTimeSeconds = 60)))

        prefill(ongoing, history)

        val set = sets(ongoing, plank).single()
        assertThat(set.getRequiredTimeSeconds()).isEqualTo(60)
        assertThat(set.getReps()).isEqualTo(-1)
    }

    @Test
    fun `leaves reps empty when the historic set only has a weight`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), ELSet(weight = 65f))))
        val ongoing = workout(now, bench to listOf(ELSet(), ELSet()))

        prefill(ongoing, history)

        assertThat(reps(ongoing, bench)).containsExactly(10, -1).inOrder()
        assertThat(weights(ongoing, bench)).containsExactly(60f, 65f).inOrder()
    }

    @Test
    fun `1RM goal takes the weight from the 1RM and the reps from the last session`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        val history = listOf(workout(at(2026, 9, 29), bench to listOf(loggedSet(5, 90f))))
        val ongoing = workout(now, bench to listOf(ELSet()))

        prefill(ongoing, history)

        assertThat(weights(ongoing, bench)).containsExactly(82f)
        assertThat(reps(ongoing, bench)).containsExactly(5)
    }

    // Targeted prefill

    @Test
    fun `only prefills the exercises passed in`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f)), squat to listOf(loggedSet(5, 100f))))
        // The user cleared bench, then added squat
        val ongoing = workout(now, bench to listOf(ELSet()), squat to listOf(ELSet()))

        WorkoutPrefillController.prefill(ongoing.findExercise(squat)!!, history, now)

        assertThat(reps(ongoing, bench)).containsExactly(-1)
        assertThat(weights(ongoing, bench)).containsExactly(-1f)
        assertThat(reps(ongoing, squat)).containsExactly(5)
        assertThat(weights(ongoing, squat)).containsExactly(100f)
    }

    @Test
    fun `returns what each exercise was prefilled from`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        val ongoing = workout(now, bench to listOf(ELSet()), squat to listOf(ELSet()))

        val sources = prefill(ongoing, history)

        assertThat(sources.keys).containsExactly("bench", "squat")
        assertThat(sources.getValue("bench").lastSession).isNotNull()
        assertThat(sources.getValue("squat").lastSession).isNull()
    }

    @Test
    fun `building the sources doesn't change the sets`() {
        // As when resuming a workout, where the user may have cleared prefilled values
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        val ongoing = workout(now, bench to listOf(ELSet()))

        val sources = WorkoutPrefillController.buildPrefillSources(listOf(bench), history, now)

        assertThat(sources.getValue("bench").lastSession).isNotNull()
        assertThat(reps(ongoing, bench)).containsExactly(-1)
        assertThat(weights(ongoing, bench)).containsExactly(-1f)
    }

    @Test
    fun `applying fills sets added after the sources were built`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), loggedSet(8, 65f))))
        val ongoing = workout(now, bench to listOf(ELSet()))
        val sources = WorkoutPrefillController.buildPrefillSources(listOf(bench), history, now)
        // The user added a set while history was loading
        ongoing.findExercise(bench)!!.single().sets.add(ELSet())

        WorkoutPrefillController.applyPrefill(ongoing.findExercise(bench)!!, sources)

        assertThat(reps(ongoing, bench)).containsExactly(10, 8).inOrder()
    }

    @Test
    fun `applying skips exercises without a source`() {
        val ongoing = workout(now, bench to listOf(ELSet()))

        WorkoutPrefillController.applyPrefill(ongoing.findExercise(bench)!!, emptyMap())

        assertThat(reps(ongoing, bench)).containsExactly(-1)
    }

    // Added sets

    @Test
    fun `an added set takes the same set of the last session instead of the copied one`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), loggedSet(8, 65f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)
        // A copy of set 1, as made by ELExerciseGroup.setAdd
        val added = loggedSet(10, 60f)

        assertThat(WorkoutPrefillController.prefillAddedSet(source, added, 1)).isTrue()

        assertThat(added.getReps()).isEqualTo(8)
        assertThat(added.getWeight()).isEqualTo(65f)
    }

    @Test
    fun `an added set past the last session keeps the copied values`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)
        val added = loggedSet(12, 50f)

        assertThat(WorkoutPrefillController.prefillAddedSet(source, added, 1)).isFalse()

        assertThat(added.getReps()).isEqualTo(12)
        assertThat(added.getWeight()).isEqualTo(50f)
    }

    @Test
    fun `an added set keeps the copied values when the last session skipped that set`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), ELSet(weight = 65f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)
        val added = loggedSet(10, 60f)

        assertThat(WorkoutPrefillController.prefillAddedSet(source, added, 1)).isFalse()

        assertThat(added.getReps()).isEqualTo(10)
    }

    @Test
    fun `an added set can switch from reps to time`() {
        val history = listOf(workout(at(2026, 9, 28), plank to listOf(ELSet(reps = 10), ELSet(timeSeconds = 45))))
        val source = WorkoutPrefillController.buildPrefillSource(plank, history, now)
        val added = ELSet(reps = 10)

        WorkoutPrefillController.prefillAddedSet(source, added, 1)

        assertThat(added.getTimeSeconds()).isEqualTo(45)
        assertThat(added.getReps()).isEqualTo(-1)
    }

    @Test
    fun `an added set drops the template target copied from the previous set`() {
        val history = listOf(workout(at(2026, 9, 28), bench to listOf(loggedSet(10, 60f), loggedSet(8, 65f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)
        val added = ELSet(requiredReps = 10, reps = 10)

        WorkoutPrefillController.prefillAddedSet(source, added, 1)

        assertThat(added.isRequiredRepsEntered()).isFalse()
        assertThat(added.getReps()).isEqualTo(8)
    }

    @Test
    fun `an added set with a 1RM goal targets the 1RM`() {
        SettingsManager.manager.setMuscleGoal(MuscleGoal.GROWTH)
        val history = listOf(workout(at(2026, 9, 29), bench to listOf(loggedSet(5, 90f), loggedSet(5, 85f))))
        val source = WorkoutPrefillController.buildPrefillSource(bench, history, now)
        // Copied from set 1 after the user lowered its weight
        val added = loggedSet(6, 70f)

        WorkoutPrefillController.prefillAddedSet(source, added, 1)

        assertThat(added.getWeight()).isEqualTo(82f)
        assertThat(added.getReps()).isEqualTo(5)
    }

    // Helpers

    private fun prefill(workout: ELWorkout, history: List<ELWorkout>): Map<String, BaseWorkoutPrefillController.PrefillSource> {
        return WorkoutPrefillController.prefill(workout.getExerciseGroups().flatMap { it.exercises }, history, now)
    }

    private fun sets(workout: ELWorkout, exercise: ELExercise): List<ELSet> {
        return workout.findExercise(exercise)!!.single().sets
    }

    private fun weights(workout: ELWorkout, exercise: ELExercise): List<Float> {
        return sets(workout, exercise).map { it.getWeight() }
    }

    private fun reps(workout: ELWorkout, exercise: ELExercise): List<Int> {
        return sets(workout, exercise).map { it.getReps() }
    }

    private fun times(workout: ELWorkout, exercise: ELExercise): List<Int> {
        return sets(workout, exercise).map { it.getTimeSeconds() }
    }
}
