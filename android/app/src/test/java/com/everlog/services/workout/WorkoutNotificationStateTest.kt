package com.everlog.services.workout

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.ELWorkoutState
import com.everlog.android.common.datatest.plannedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.workout.data.notification.ShownSet
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.everlog.services.workout.WorkoutNotificationState.Done
import com.everlog.services.workout.WorkoutNotificationState.NextSet
import com.everlog.services.workout.WorkoutNotificationState.NoExercises
import com.everlog.services.workout.WorkoutNotificationState.Rest
import com.everlog.services.workout.WorkoutNotificationState.RestTimer
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutNotificationStateTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val squat = ELExercise(uuid = "squat", name = "Squat")
    private val plank = ELExercise(uuid = "plank", name = "Plank")

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
        SettingsManager.manager.setWeightUnit(WeightUnit.KILOGRAM)
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    // State selection

    @Test
    fun `no workout shows no exercises`() {
        assertThat(WorkoutNotificationState.from(null, null)).isEqualTo(NoExercises)
    }

    @Test
    fun `workout without exercises shows no exercises`() {
        assertThat(WorkoutNotificationState.from(workout(0), null)).isEqualTo(NoExercises)
    }

    @Test
    fun `workout with every set complete shows done`() {
        val workout = workout(0, bench to listOf(completedSet(), completedSet()))

        assertThat(WorkoutNotificationState.from(workout, null)).isEqualTo(Done)
    }

    @Test
    fun `rest timer takes priority over the next set`() {
        val workout = workout(0, bench to listOf(plannedSet()))

        val state = WorkoutNotificationState.from(workout, RestTimer(remainingSeconds = 45, remainingPercent = 75))

        assertThat(state).isInstanceOf(Rest::class.java)
        state as Rest
        assertThat(state.remainingSeconds).isEqualTo(45)
        assertThat(state.remainingPercent).isEqualTo(75)
        assertThat(state.upNext?.exerciseName).isEqualTo("Bench press")
    }

    @Test
    fun `rest after the last set has nothing up next`() {
        val workout = workout(0, bench to listOf(completedSet()))

        val state = WorkoutNotificationState.from(workout, RestTimer(remainingSeconds = 45, remainingPercent = 75)) as Rest

        assertThat(state.upNext).isNull()
    }

    // Next set

    @Test
    fun `next set is the first incomplete set`() {
        val workout = workout(0,
                bench to listOf(completedSet(), completedSet()),
                squat to listOf(completedSet(), ELSet(requiredReps = 5, reps = 5, weight = 100f), plannedSet()))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.exerciseName).isEqualTo("Squat")
        assertThat(state.setNumber).isEqualTo(2)
        assertThat(state.totalSets).isEqualTo(3)
        assertThat(state.weightKg).isEqualTo(100f)
        assertThat(state.reps).isEqualTo(5)
    }

    @Test
    fun `a set chosen on the screen shows rather than the next one`() {
        val workout = workout(0, bench to listOf(plannedSet(), plannedSet()), squat to listOf(plannedSet()))

        val shown = ShownSet(ELWorkoutState(groupIndex = 1, setIndex = 0, exerciseIndex = 0), startsTimer = true)
        val state = WorkoutNotificationState.from(workout, null, shown) as NextSet

        assertThat(state.exerciseName).isEqualTo("Squat")
        assertThat(state.startsTimer).isTrue()
    }

    @Test
    fun `a chosen set that's no longer in the workout shows the next one`() {
        val workout = workout(0, bench to listOf(plannedSet(), plannedSet()))

        val shown = ShownSet(ELWorkoutState(groupIndex = 3, setIndex = 0, exerciseIndex = 0), startsTimer = true)
        val state = WorkoutNotificationState.from(workout, null, shown) as NextSet

        assertThat(state.exerciseName).isEqualTo("Bench press")
        assertThat(state.setNumber).isEqualTo(1)
        assertThat(state.startsTimer).isFalse()
    }

    @Test
    fun `next set follows the most recently completed set`() {
        // Set 1 was skipped and set 2 completed, so set 3 is next rather than set 1
        val workout = workout(0, bench to listOf(plannedSet(), completedSet(), plannedSet()))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.setNumber).isEqualTo(3)
    }

    @Test
    fun `rest shows the set after the one just completed up next`() {
        val workout = workout(0,
                bench to listOf(completedSet(completedDate = 1L), plannedSet()),
                squat to listOf(plannedSet(), completedSet(completedDate = 2L), plannedSet()))

        val state = WorkoutNotificationState.from(workout, RestTimer(remainingSeconds = 45, remainingPercent = 100)) as Rest

        assertThat(state.upNext?.exerciseName).isEqualTo("Squat")
        assertThat(state.upNext?.setNumber).isEqualTo(3)
    }

    @Test
    fun `next set wraps round to skipped sets once the later ones are complete`() {
        val workout = workout(0,
                bench to listOf(plannedSet(), completedSet(completedDate = 1L)),
                squat to listOf(completedSet(completedDate = 2L)))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.exerciseName).isEqualTo("Bench press")
        assertThat(state.setNumber).isEqualTo(1)
    }

    @Test
    fun `rep based set shows reps, with no weight as empty`() {
        val workout = workout(0, bench to listOf(ELSet(requiredReps = 8, reps = 8)))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.timed).isFalse()
        assertThat(state.reps).isEqualTo(8)
        assertThat(state.weightKg).isNull()
    }

    @Test
    fun `values not entered are empty, as the workout table shows them`() {
        // Older app versions saved a missing weight as -1
        val workout = workout(0, bench to listOf(ELSet(weight = -1f)))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.timed).isFalse()
        assertThat(state.reps).isNull()
        assertThat(state.weightKg).isNull()
    }

    @Test
    fun `time based set shows time`() {
        val workout = workout(0, plank to listOf(ELSet(requiredTimeSeconds = 40, timeSeconds = 40)))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.timed).isTrue()
        assertThat(state.timeSeconds).isEqualTo(40)
        assertThat(state.timerRunning).isFalse()
    }

    @Test
    fun `running exercise timer shows the remaining time`() {
        val set = ELSet(requiredTimeSeconds = 40, timeSeconds = 40)
        set.remainingTimeSeconds = 12
        val workout = workout(0, plank to listOf(set))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.timeSeconds).isEqualTo(12)
        assertThat(state.timerRunning).isTrue()
    }

    @Test
    fun `super set reports the exercise position within the group`() {
        val group = ELExerciseGroup(type = "SUPER", exercises = mutableListOf(
                ELRoutineExercise(bench.uuid, bench, mutableListOf(completedSet(), plannedSet())),
                ELRoutineExercise(squat.uuid, squat, mutableListOf(completedSet(), plannedSet()))))
        // Complete the bench press in the second round, so squat is next
        group.exercises[0].sets[1].updateCompletedDate(1L)
        val workout = ELWorkout(routine = ELRoutine(exerciseGroups = mutableListOf(group)))

        val state = WorkoutNotificationState.from(workout, null) as NextSet

        assertThat(state.exerciseName).isEqualTo("Squat")
        assertThat(state.exercisePosition).isEqualTo(2)
        assertThat(state.exercisesInGroup).isEqualTo(2)
        assertThat(state.setType).isEqualTo("SUPER")
        assertThat(state.setNumber).isEqualTo(2)
        assertThat(state.totalSets).isEqualTo(2)
    }

    private fun completedSet(completedDate: Long = 1L) = ELSet(requiredReps = 8, reps = 8, weight = 60f, completedDate = completedDate)
}
