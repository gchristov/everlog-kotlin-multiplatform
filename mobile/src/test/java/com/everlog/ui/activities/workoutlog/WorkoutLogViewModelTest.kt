package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.set
import com.everlog.config.AppConfig
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.testutil.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutLogViewModelTest {

    private val now = 1_000_000L
    // The clock the view-model reads, moved on by tests that let time pass
    private var time = now
    private val store = FakeStore()
    private val settings = FakeSettings()
    private val analytics = RecordingAnalytics()
    private val alerts = CountingAlerts()
    private val notification = FakeNotification()

    // Bench press, 2 sets of 8, then a superset of fly and raise, 2 rounds of 12
    private fun template() = ELWorkout(
        uuid = "workout",
        createdDate = now - 60_000,
        routine = ELRoutine(
            name = "Push",
            exerciseGroups = mutableListOf(
                group("SINGLE", "bench" to 8, sets = 2),
                group("SUPER", "fly" to 12, "raise" to 12, sets = 2),
            ),
        ),
    )

    private fun viewModel(launched: ELWorkout = template()) = WorkoutLogViewModel(
        dispatcher = FakeCoroutineDispatcher,
        workout = WorkoutLogViewModel.load(launched, store),
        fromPlan = false,
        store = store,
        settings = settings,
        analytics = analytics,
        alerts = alerts,
        notification = notification,
        clock = { time },
        tickMillis = null,
    )

    private val WorkoutLogViewModel.blocks get() = state.value.blocks

    // The set at these positions: exercise group, round, exercise in the group
    private fun WorkoutLogViewModel.ref(block: Int, round: Int, exercise: Int = 0) =
        SetRef(blocks[block].id, blocks[block].rounds[round].id, exercise)

    private fun WorkoutLogViewModel.set(block: Int, round: Int, exercise: Int = 0) = blocks.set(ref(block, round, exercise))!!

    private fun WorkoutLogViewModel.type(text: String) = text.forEach { if (it == '.') onDecimal() else onDigit(it) }

    // Starting and resuming

    @Test
    fun `a new workout starts on its first set, with the template's targets as values, saved straight away`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(0, 0))
        assertThat(viewModel.state.value.selected).isEqualTo(viewModel.ref(0, 0))
        assertThat(viewModel.set(0, 0).reps).isEqualTo(8)
        assertThat(store.ongoing?.uuid).isEqualTo("workout")
    }

    @Test
    fun `a resumed workout carries on after the set completed last`() {
        val first = viewModel()
        first.onTick(first.ref(0, 0))
        store.saved = store.ongoing

        val resumed = viewModel()

        assertThat(resumed.set(0, 0).done).isTrue()
        assertThat(resumed.state.value.current).isEqualTo(resumed.ref(0, 1))
    }

    // Typing

    @Test
    fun `the first key replaces the value and the next ones add to it`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")

        assertThat(viewModel.set(0, 0).reps).isEqualTo(12)
        assertThat(viewModel.state.value.typed).isEqualTo("12")
    }

    @Test
    fun `a weight takes one decimal point and is stored in kg`() {
        settings.unit = WeightUnit.POUND
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("13.5.")
        viewModel.onDigit('0')

        assertThat(viewModel.state.value.typed).isEqualTo("13.50")
        assertThat(displayWeight(viewModel.set(0, 0).weightKg!!, WeightUnit.POUND)).isWithin(0.01f).of(13.5f)
    }

    @Test
    fun `reps take no decimal point`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("1.0")

        assertThat(viewModel.set(0, 0).reps).isEqualTo(10)
    }

    @Test
    fun `backspace takes the last digit off the value, and off nothing clears it`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onBackspace()
        assertThat(viewModel.set(0, 0).reps).isNull()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.type("15")
        viewModel.onBackspace()
        assertThat(viewModel.set(0, 1).reps).isEqualTo(1)
    }

    @Test
    fun `− and + step by a rep, or by the weight step, and never below nothing`() {
        settings.step = 2.5f
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.onStep(1)
        viewModel.onStep(1)
        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(5f)

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        repeat(9) { viewModel.onStep(-1) }
        assertThat(viewModel.set(0, 0).reps).isNull()
    }

    @Test
    fun `switching to time moves the keypad there, and time is typed like a microwave's`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("130")

        assertThat(viewModel.state.value.focus?.field).isEqualTo(ValueField.Time)
        assertThat(viewModel.set(0, 0).timeSeconds).isEqualTo(90)
    }

    // The keypad's main key

    @Test
    fun `next goes from reps to weight, whose key completes the set and opens the next set's reps`() {
        val viewModel = viewModel()
        viewModel.onShowKeypad()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Next)

        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.CompleteSet)

        viewModel.onMainKey()
        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
    }

    @Test
    fun `a done set's key is Done, which closes the keypad on the current set`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)

        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus).isNull()
        assertThat(viewModel.state.value.selected).isEqualTo(viewModel.ref(0, 1))
    }

    @Test
    fun `completing the last set closes the keypad`() {
        val viewModel = viewModel()
        listOf(viewModel.ref(0, 0), viewModel.ref(0, 1), viewModel.ref(1, 0, 0), viewModel.ref(1, 0, 1), viewModel.ref(1, 1, 0))
            .forEach(viewModel::onTick)
        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 1, 1), ValueField.Weight))

        viewModel.onMainKey()

        assertThat(viewModel.state.value.focus).isNull()
        assertThat(viewModel.state.value.unfinishedSets).isEqualTo(0)
    }

    // Ticks and the selection

    @Test
    fun `a tick completes the set, the selection moves on, and a second tick undoes it`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.set(0, 0).completedDate).isEqualTo(now)
        assertThat(viewModel.state.value.selected).isEqualTo(viewModel.ref(0, 1))

        viewModel.onTick(viewModel.ref(0, 0))
        assertThat(viewModel.set(0, 0).done).isFalse()
        assertThat(viewModel.set(0, 0).reps).isEqualTo(8)
        assertThat(analytics.events).containsExactly("set_completed SINGLE", "rest_started", "set_edited SINGLE").inOrder()
    }

    @Test
    fun `a superset goes exercise by exercise and logs the round once it's done`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onTick(viewModel.ref(0, 1))
        viewModel.onCompleteTarget()
        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(1, 0, 1))

        viewModel.onCompleteTarget()
        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(1, 1, 0))
        assertThat(analytics.events.filter { it.startsWith("set_") }.takeLast(3))
            .containsExactly("set_completed SUPER", "set_completed SUPER", "set_group_completed SUPER 2").inOrder()
    }

    @Test
    fun `the open keypad follows a tick to its set`() {
        val viewModel = viewModel()
        viewModel.onShowKeypad()
        viewModel.onTick(viewModel.ref(0, 1))

        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        assertThat(viewModel.set(0, 1).done).isTrue()
    }

    @Test
    fun `an exercise added to an empty workout is selected`() {
        val viewModel = viewModel(ELWorkout(uuid = "empty", routine = ELRoutine(name = "Quick")))
        assertThat(viewModel.state.value.selected).isNull()

        viewModel.onExercisesPicked(listOf(group("SINGLE", "squat" to 5, sets = 1)))

        assertThat(viewModel.state.value.selected).isEqualTo(viewModel.ref(0, 0))
        assertThat(analytics.events).contains("exercises_added")
    }

    // Timers

    @Test
    fun `a done set starts its rest, which sounds when it runs out`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        assertThat(viewModel.state.value.timer).isEqualTo(WorkoutTimer.Rest(endsAt = now + 90_000, totalSeconds = 90))

        time = now + 90_000
        viewModel.tick()

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(alerts.count).isEqualTo(1)
    }

    @Test
    fun `a superset rests after its round, not between its exercises`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(1, 0, 0))
        assertThat(viewModel.state.value.timer).isNull()

        viewModel.onTick(viewModel.ref(1, 0, 1))
        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
    }

    @Test
    fun `−10s and +10s change this rest and every one after it`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onTimerStep(1)

        assertThat(viewModel.state.value.timer?.totalSeconds).isEqualTo(100)
        assertThat(viewModel.blocks.map { it.restSeconds }).containsExactly(100, 100)
    }

    @Test
    fun `taking a rest down to nothing ends it, and the rests after keep theirs`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        repeat(10) { viewModel.onTimerStep(-1) }

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(viewModel.blocks.map { it.restSeconds }).containsExactly(10, 10)
    }

    @Test
    fun `skipping a rest ends it without a sound`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onTimerEnd()

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(alerts.count).isEqualTo(0)
        assertThat(analytics.events.last()).isEqualTo("rest_stopped")
    }

    @Test
    fun `the last set of the workout doesn't start a rest`() {
        val viewModel = viewModel()
        viewModel.blocks.indices.forEach { b ->
            viewModel.blocks[b].rounds.indices.forEach { r ->
                viewModel.blocks[b].exercises.indices.forEach { e -> viewModel.onTick(viewModel.ref(b, r, e)) }
            }
            viewModel.onTimerEnd()
        }

        assertThat(viewModel.state.value.timer).isNull()
    }

    @Test
    fun `a timed set's key starts its timer, which sounds when it runs out and leaves the set to be ticked`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("45")
        viewModel.onMainKey()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.StartTimer)

        viewModel.onMainKey()
        assertThat(viewModel.state.value.timer).isEqualTo(WorkoutTimer.SetTimer(viewModel.ref(0, 0), endsAt = now + 45_000, totalSeconds = 45))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.CompleteSet)

        time = now + 45_000
        viewModel.tick()
        assertThat(viewModel.set(0, 0).done).isFalse()
        assertThat(viewModel.state.value.timer).isNull()
        assertThat(alerts.count).isEqualTo(1)
        assertThat(viewModel.state.value.startsTimer(viewModel.ref(0, 0))).isFalse()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.CompleteSet)

        viewModel.onMainKey()
        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
    }

    @Test
    fun `+10s on a set timer adds to the set's time`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onTimerStep(1)

        assertThat(viewModel.set(0, 0).timeSeconds).isEqualTo(40)
        assertThat(viewModel.state.value.timer?.endsAt).isEqualTo(now + 40_000)
    }

    @Test
    fun `stopping a set timer leaves the set to be ticked, without a sound`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onTimerEnd()

        assertThat(viewModel.set(0, 0).done).isFalse()
        assertThat(viewModel.set(0, 0).timeSeconds).isEqualTo(30)
        assertThat(viewModel.state.value.targetStartsTimer).isFalse()
        assertThat(alerts.count).isEqualTo(0)
        assertThat(analytics.events).containsAtLeast("set_timer_started", "set_timer_stopped").inOrder()
    }

    @Test
    fun `completing another set ends a running set timer`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onTick(viewModel.ref(0, 1))

        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
        assertThat(viewModel.set(0, 0).done).isFalse()
    }

    @Test
    fun `a timed set without a time opens the keypad on it rather than starting`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.onHideKeypad()

        viewModel.onStartSetTimer(viewModel.ref(0, 0))

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 0), ValueField.Time))
    }

    // Adding and deleting sets

    @Test
    fun `an added set copies the last set's values, not done`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 1))
        viewModel.onAddSet(viewModel.blocks[0].id)

        assertThat(viewModel.blocks[0].rounds).hasSize(3)
        assertThat(viewModel.set(0, 2).reps).isEqualTo(8)
        assertThat(viewModel.set(0, 2).done).isFalse()
        assertThat(analytics.events).contains("set_added")
    }

    @Test
    fun `no more sets than the old screen's limit`() {
        val viewModel = viewModel()
        repeat(20) { viewModel.onAddSet(viewModel.blocks[0].id) }

        assertThat(viewModel.blocks[0].rounds).hasSize(AppConfig.configuration.maxExerciseSets)
        assertThat(viewModel.state.value.message).isEqualTo(WorkoutLogMessage.TooManySets(AppConfig.configuration.maxExerciseSets))
    }

    @Test
    fun `a deleted set goes, the current set moves on, and undo puts both back`() {
        val viewModel = viewModel()
        val first = viewModel.ref(0, 0)
        viewModel.onDeleteSet(first)

        assertThat(viewModel.blocks[0].rounds).hasSize(1)
        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(0, 0))
        assertThat(analytics.events).contains("set_deleted")

        viewModel.onUndoDelete()
        assertThat(viewModel.blocks[0].rounds).hasSize(2)
        assertThat(viewModel.state.value.current).isEqualTo(first)
        assertThat(viewModel.state.value.deletion).isNull()
    }

    @Test
    fun `deleting an exercise's last set deletes the exercise, and undo puts it back`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(0, 1))
        viewModel.onDeleteSet(viewModel.ref(0, 0))
        assertThat(viewModel.blocks.map { it.uuid }).containsExactly("group-fly")

        viewModel.onUndoDelete()
        assertThat(viewModel.blocks.map { it.uuid }).containsExactly("group-bench", "group-fly").inOrder()
    }

    @Test
    fun `deleting one exercise's set in a superset keeps the other's`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(1, 0, 1))

        assertThat(viewModel.blocks[1].rounds).hasSize(2)
        assertThat(viewModel.set(1, 0, 1).removed).isTrue()
        assertThat(viewModel.set(1, 0, 0).removed).isFalse()
    }

    @Test
    fun `any other change ends the undo`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(0, 0))
        viewModel.onTick(viewModel.ref(1, 0, 0))

        assertThat(viewModel.state.value.deletion).isNull()
    }

    @Test
    fun `deleting a set closes the keypad on it and stops its timer`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.onDeleteSet(viewModel.ref(0, 0))

        assertThat(viewModel.state.value.focus).isNull()
        assertThat(viewModel.state.value.timer).isNull()
    }

    // Exercises

    @Test
    fun `deleting an exercise asks first, then its sets go and the current set moves on`() {
        val viewModel = viewModel()
        val bench = viewModel.blocks[0].id
        viewModel.onDeleteExercise(bench)
        assertThat(viewModel.state.value.dialog).isEqualTo(WorkoutLogDialog.DeleteExercise(bench))
        assertThat(viewModel.blocks).hasSize(2)

        viewModel.onDeleteExerciseConfirmed(bench)
        assertThat(viewModel.blocks.map { it.uuid }).containsExactly("group-fly")
        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(0, 0, 0))
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `deleting the last exercise ends the rest`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onDeleteExerciseConfirmed(viewModel.blocks[0].id)
        assertThat(viewModel.state.value.timer).isNotNull()

        viewModel.onDeleteExerciseConfirmed(viewModel.blocks[0].id)
        assertThat(viewModel.state.value.timer).isNull()
    }

    @Test
    fun `an exercise changed meanwhile shows as it is now, and is saved`() {
        val viewModel = viewModel()
        viewModel.onExerciseChanged(ELExercise(uuid = "fly", name = "Cable fly"))

        assertThat(viewModel.blocks[1].exercises.first().exercise?.name).isEqualTo("Cable fly")
        assertThat(store.ongoing?.getExerciseGroups()?.get(1)?.exercises?.first()?.exercise?.name).isEqualTo("Cable fly")
    }

    // Edit workout and the workout's options

    @Test
    fun `edit workout closes the keypad, and back ends it before anything else`() {
        val viewModel = viewModel()
        viewModel.onShowKeypad()
        viewModel.onEditWorkout()
        assertThat(viewModel.state.value.editing).isTrue()
        assertThat(viewModel.state.value.focus).isNull()

        viewModel.onBack()
        assertThat(viewModel.state.value.editing).isFalse()
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `an exercise moved keeps its sets, and the order is saved`() {
        val viewModel = viewModel()
        viewModel.onMoveExercise(0, 1)

        assertThat(viewModel.blocks.map { it.uuid }).containsExactly("group-fly", "group-bench").inOrder()
        assertThat(store.ongoing?.getExerciseGroups()?.map { it.uuid }).containsExactly("group-fly", "group-bench").inOrder()
    }

    @Test
    fun `deleting the last exercise while editing ends editing`() {
        val viewModel = viewModel()
        viewModel.onEditWorkout()
        viewModel.onDeleteExerciseConfirmed(viewModel.blocks[0].id)
        assertThat(viewModel.state.value.editing).isTrue()

        viewModel.onDeleteExerciseConfirmed(viewModel.blocks[0].id)
        assertThat(viewModel.state.value.editing).isFalse()
    }

    @Test
    fun `the rest from the options changes every exercise's by 10s, not below 10s`() {
        val viewModel = viewModel()
        viewModel.onRestTimerSettings()
        viewModel.onRestStep(1)
        assertThat(viewModel.state.value.restSeconds).isEqualTo(100)
        assertThat(viewModel.blocks.map { it.restSeconds }).containsExactly(100, 100)
        assertThat(analytics.events.last()).isEqualTo("rest_changed 100")

        repeat(20) { viewModel.onRestStep(-1) }
        assertThat(viewModel.state.value.restSeconds).isEqualTo(10)
    }

    // The notification

    @Test
    fun `the notification starts and stops with the screen, and is sent every change`() {
        val viewModel = viewModel()
        viewModel.onScreenStarted()
        assertThat(notification.started?.uuid).isEqualTo("workout")

        viewModel.onTick(viewModel.ref(0, 0))
        assertThat(notification.shown?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.isComplete()).isTrue()

        viewModel.onScreenClosed()
        assertThat(notification.stopped).isTrue()
    }

    @Test
    fun `the notification is sent the workout again when the screen comes back, as a setting may have changed`() {
        val viewModel = viewModel()
        notification.shown = null
        viewModel.onResume()

        assertThat(notification.shown?.uuid).isEqualTo("workout")
    }

    @Test
    fun `the notification counts a rest down`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        time = now + 45_000
        viewModel.tick()

        assertThat(notification.rest).isEqualTo(50 to 45)
    }

    @Test
    fun `the notification shows a set timer's seconds left on its set`() {
        val viewModel = timedSetRunning(seconds = "30")
        time = now + 10_000
        viewModel.tick()

        assertThat(notification.shown?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.remainingTimeSeconds).isEqualTo(20)
    }

    @Test
    fun `the notification's button completes its set, logged as from the notification`() {
        val viewModel = viewModel()
        viewModel.onNotificationCompleteSet(position(group = 0, set = 0, exercise = 0))

        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
        assertThat(analytics.events.first()).isEqualTo("set_completed SINGLE notification")
    }

    @Test
    fun `the notification's buttons point at a superset exercise by its place in the round`() {
        val viewModel = viewModel()
        viewModel.onNotificationCompleteSet(position(group = 1, set = 1, exercise = 1))

        assertThat(viewModel.set(1, 1, 1).done).isTrue()
    }

    @Test
    fun `the notification's ± change weight by the weight step and reps by one`() {
        settings.step = 2.5f
        val viewModel = viewModel()
        viewModel.onNotificationWeight(position(0, 0, 0), 1)
        viewModel.onNotificationReps(position(0, 0, 0), -1)

        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(2.5f)
        assertThat(viewModel.set(0, 0).reps).isEqualTo(7)
    }

    @Test
    fun `the notification starts and stops a set timer and stops a rest, which it logs itself`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("30")
        viewModel.onHideKeypad()

        viewModel.onNotificationSetTimer(position(0, 0, 0), start = true)
        assertThat(viewModel.state.value.isTiming(viewModel.ref(0, 0))).isTrue()
        viewModel.onNotificationSetTimer(position(0, 0, 0), start = false)
        assertThat(viewModel.state.value.timer).isNull()

        viewModel.onTick(viewModel.ref(0, 1))
        viewModel.onNotificationRestStop()
        assertThat(viewModel.state.value.timer).isNull()
        assertThat(analytics.events).containsNoneOf("set_timer_started", "set_timer_stopped", "rest_stopped")
    }

    // Leaving and finishing

    @Test
    fun `back closes the keypad first, then asks about leaving`() {
        val viewModel = viewModel()
        viewModel.onShowKeypad()
        viewModel.onBack()
        assertThat(viewModel.state.value.focus).isNull()
        assertThat(viewModel.state.value.dialog).isNull()

        viewModel.onBack()
        assertThat(viewModel.state.value.dialog).isEqualTo(WorkoutLogDialog.Discard(setsCompleted = 0, untickedSaved = 6))
    }

    @Test
    fun `keeping going leaves the workout as it was`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onClose()
        viewModel.onDialogDismissed()

        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.exit).isNull()
        assertThat(analytics.events.takeLast(2)).containsExactly("discard_prompt_shown 1", "discard_prompt_cancelled").inOrder()
    }

    @Test
    fun `discarding clears the ongoing workout without saving it`() {
        val viewModel = viewModel()
        viewModel.onClose()
        viewModel.onDiscardConfirmed()

        assertThat(store.ongoing).isNull()
        assertThat(store.finished).isNull()
        assertThat(viewModel.state.value.exit).isEqualTo(WorkoutLogExit.Discarded)
    }

    @Test
    fun `finishing with sets not ticked asks first, then saves the workout as it is`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onFinish()
        assertThat(viewModel.state.value.dialog).isEqualTo(WorkoutLogDialog.Finish)
        assertThat(store.finished).isNull()

        viewModel.onFinishConfirmed()

        val finished = store.finished!!
        assertThat(finished.completedDate).isEqualTo(now)
        assertThat(finished.getExerciseGroups().first().exercises.first().sets.first().isComplete()).isTrue()
        assertThat(store.ongoing).isNull()
        assertThat(viewModel.state.value.exit).isEqualTo(WorkoutLogExit.Finished(finished))
        assertThat(analytics.events.last()).isEqualTo("workout_completed")
    }

    @Test
    fun `finishing from the discard prompt doesn't ask again`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onClose()
        viewModel.onDiscardFinish()

        assertThat(store.finished).isNotNull()
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `every change is saved as the ongoing workout`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("6")

        assertThat(store.ongoing?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.getReps()).isEqualTo(6)
    }

    private fun position(group: Int, set: Int, exercise: Int) =
        ELWorkoutState(groupIndex = group, setIndex = set, exerciseIndex = exercise)

    // Bench press's first set switched to time and its timer running
    private fun timedSetRunning(seconds: String): WorkoutLogViewModel {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type(seconds)
        viewModel.onStartSetTimer(viewModel.ref(0, 0))
        return viewModel
    }

    private fun group(type: String, vararg exercises: Pair<String, Int>, sets: Int) = ELExerciseGroup(
        uuid = "group-${exercises.first().first}",
        type = type,
        exercises = exercises.map { (name, reps) ->
            ELRoutineExercise(name, ELExercise(uuid = name, name = name), MutableList(sets) { ELSet(requiredReps = reps) })
        }.toMutableList(),
        restTimeSeconds = 90,
    )

    private class FakeStore : WorkoutLogStore {
        // The ongoing workout as saved before the screen opened, if it's being resumed
        var saved: ELWorkout? = null
        var ongoing: ELWorkout? = null
        var finished: ELWorkout? = null

        override fun savedCopyOf(workout: ELWorkout): ELWorkout? = saved?.takeIf { it.uuid == workout.uuid }

        override fun saveOngoing(workout: ELWorkout) {
            ongoing = workout
        }

        override fun clearOngoing() {
            ongoing = null
        }

        override fun saveFinished(workout: ELWorkout, fromPlan: Boolean) {
            finished = workout
        }
    }

    private class FakeSettings : WorkoutLogSettings {
        var unit = WeightUnit.KILOGRAM
        var step = 2.5f

        override fun weightUnit() = unit

        override fun weightIncrease() = step

        override fun keepScreenOn() = false
    }

    private class FakeNotification : WorkoutLogNotification {
        var started: ELWorkout? = null
        var stopped = false
        // The workout it was last sent
        var shown: ELWorkout? = null
        var rest: Pair<Int, Int>? = null

        override fun start(workout: ELWorkout) {
            started = workout
        }

        override fun stop() {
            stopped = true
        }

        override fun update(workout: ELWorkout) {
            shown = workout
            rest = null
        }

        override fun showRest(remainingPercent: Int, remainingSeconds: Int) {
            rest = remainingPercent to remainingSeconds
        }
    }

    private class CountingAlerts : WorkoutLogAlerts {
        var count = 0

        override fun timerRanOut() {
            count++
        }
    }

    private class RecordingAnalytics : WorkoutLogAnalytics {
        val events = mutableListOf<String>()

        override fun screenViewed() {
            events += "screen"
        }

        override fun setCompleted(type: ELSetType?, timed: Boolean, fromNotification: Boolean) {
            events += "set_completed $type" + if (fromNotification) " notification" else ""
        }

        override fun setGroupCompleted(type: ELSetType?, exercises: Int, fromNotification: Boolean) {
            events += "set_group_completed $type $exercises" + if (fromNotification) " notification" else ""
        }

        override fun setEdited(type: ELSetType?, timed: Boolean) {
            events += "set_edited $type"
        }

        override fun exercisesAdded() {
            events += "exercises_added"
        }

        override fun setAdded() {
            events += "set_added"
        }

        override fun setDeleted() {
            events += "set_deleted"
        }

        override fun restChanged(seconds: Int) {
            events += "rest_changed $seconds"
        }

        override fun restStarted() {
            events += "rest_started"
        }

        override fun restStopped() {
            events += "rest_stopped"
        }

        override fun setTimerStarted() {
            events += "set_timer_started"
        }

        override fun setTimerStopped() {
            events += "set_timer_stopped"
        }

        override fun workoutCompleted() {
            events += "workout_completed"
        }

        override fun workoutStopped() {
            events += "workout_stopped"
        }

        override fun discardPromptShown(setsCompleted: Int) {
            events += "discard_prompt_shown $setsCompleted"
        }

        override fun discardPromptFinished() {
            events += "discard_prompt_finished"
        }

        override fun discardPromptCancelled() {
            events += "discard_prompt_cancelled"
        }
    }
}
