package com.everlog.ui.activities.workoutlog

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELExerciseHistory
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.ELWorkoutState
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.settings.testfixtures.install
import com.everlog.android.settings.testfixtures.uninstall
import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.controllers.workoutprefill.LoadPrefillSourcesUseCase
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.WorkoutSetPrefill
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.repositories.OngoingWorkoutRepository
import com.everlog.services.workout.ShownSet
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.utils.format.displayWeight
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Before
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
    private val prefill = FakePrefill()
    private val ownUpdates = MutableSharedFlow<List<LibraryExercise>>(extraBufferCapacity = 1)

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

    private fun viewModel(launched: ELWorkout = template()): WorkoutLogViewModel {
        val loaded = WorkoutLogViewModel.load(launched, store)
        return WorkoutLogViewModel(
            dispatcher = FakeCoroutineDispatcher,
            workout = loaded.workout,
            resumed = loaded.resumed,
            fromPlan = false,
            repository = store,
            settings = settings,
            analytics = analytics,
            alerts = alerts,
            notification = notification,
            loadPrefillSources = prefill,
            prefill = prefill,
            ownExerciseUpdates = { ownUpdates },
            clock = { time },
            tickMillis = null,
        )
    }

    // ELSet reads the weight unit from the preferences
    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
        SettingsManager.manager.setWeightUnit(WeightUnit.KILOGRAM)
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

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
    fun `a value takes only as many digits as fit its cell`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12345")
        assertThat(viewModel.set(0, 0).reps).isEqualTo(123)

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("1234.567")
        assertThat(viewModel.state.value.typed).isEqualTo("1234.5")
    }

    @Test
    fun `keys do nothing while the keypad is closed`() {
        val viewModel = viewModel()
        viewModel.type("9")
        viewModel.onBackspace()
        viewModel.onStep(1)

        assertThat(viewModel.set(0, 0).reps).isEqualTo(8)
        assertThat(analytics.events.filter { "modified" in it }).isEmpty()
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
    fun `a typed value is logged once, when the keypad moves on from its cell`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")
        assertThat(analytics.events.filter { "modified" in it }).isEmpty()

        viewModel.onMainKey()
        viewModel.type("40")
        viewModel.onHideKeypad()

        assertThat(analytics.events.filter { "modified" in it }).containsExactly("reps_modified 12.0", "weight_modified 40.0").inOrder()
    }

    @Test
    fun `a cell left without typing logs nothing, and each step logs its value`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.onStep(1)
        viewModel.onStep(1)
        viewModel.onHideKeypad()

        assertThat(analytics.events.filter { "modified" in it }).containsExactly("reps_modified 9.0", "reps_modified 10.0").inOrder()
    }

    @Test
    fun `a value typed before a tick or finishing is logged before them`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("20")
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("45")
        viewModel.onFinish()

        assertThat(analytics.events.filter { "modified" in it }).containsExactly("weight_modified 20.0", "time_modified 45.0").inOrder()
        assertThat(analytics.events.indexOf("weight_modified 20.0")).isLessThan(analytics.events.indexOfFirst { it.startsWith("set_completed") })
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

    // Opening and closing the keypad

    @Test
    fun `the keypad logs how it was opened and closed, once each time`() {
        val viewModel = viewModel()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        // Moving between cells keeps it open
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.onHideKeypad()
        viewModel.onShowKeypad()
        viewModel.onBack()

        assertThat(analytics.events.filter { it.startsWith("keypad") })
            .containsExactly("keypad_shown cell", "keypad_hidden collapse", "keypad_shown button", "keypad_hidden back").inOrder()
        assertThat(viewModel.state.value.focus).isNull()
    }

    @Test
    fun `the keypad closing by itself isn't logged`() {
        val viewModel = viewModel()
        viewModel.onShowKeypad()

        viewModel.onEditWorkout()

        assertThat(viewModel.state.value.focus).isNull()
        assertThat(analytics.events.filter { it.startsWith("keypad") }).containsExactly("keypad_shown button")
    }

    @Test
    fun `hiding a closed keypad or showing an open one logs nothing`() {
        val viewModel = viewModel()
        viewModel.onHideKeypad()
        viewModel.onShowKeypad()
        val focus = viewModel.state.value.focus

        viewModel.onShowKeypad()

        assertThat(viewModel.state.value.focus).isEqualTo(focus)
        assertThat(analytics.events.filter { it.startsWith("keypad") }).containsExactly("keypad_shown button")
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
    fun `a done set's key is Done, which moves the keypad on to the set to complete next`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)

        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        assertThat(viewModel.state.value.selected).isEqualTo(viewModel.ref(0, 1))
    }

    @Test
    fun `Done closes the keypad once every set is done`() {
        val viewModel = viewModel()
        listOf(viewModel.ref(0, 0), viewModel.ref(0, 1), viewModel.ref(1, 0, 0), viewModel.ref(1, 0, 1), viewModel.ref(1, 1, 0), viewModel.ref(1, 1, 1))
            .forEach(viewModel::onTick)
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        viewModel.onMainKey()

        assertThat(viewModel.state.value.focus).isNull()
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
    fun `a timed set's key starts its timer from its time, then completes the set once the timer has run out`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("45")
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
    fun `the tick lights up with the key that presses it, a reps set's on its weight and a timed set's timer on its time`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        assertThat(viewModel.state.value.tableView("KG").armed).isNull()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.tableView("KG").armed).isEqualTo(viewModel.ref(0, 0))

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("45")
        assertThat(viewModel.state.value.tableView("KG").armed).isEqualTo(viewModel.ref(0, 1))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Weight))
        assertThat(viewModel.state.value.tableView("KG").armed).isNull()
    }

    @Test
    fun `a timed set's weight goes on to its time, whose key starts the timer`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("45")
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Next)

        viewModel.onMainKey()

        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 0), ValueField.Time))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.StartTimer)
    }

    @Test
    fun `a stopped set timer leaves its key on Complete set, on the weight too`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onTimerEnd()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.CompleteSet)

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.CompleteSet)
    }

    @Test
    fun `a done timed set's key runs its timer again, keeping it done, then finishes editing`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onMainKey()
        assertThat(viewModel.set(0, 0).done).isTrue()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Time))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.StartTimer)
        viewModel.onMainKey()
        assertThat(viewModel.state.value.timer).isEqualTo(WorkoutTimer.SetTimer(viewModel.ref(0, 0), endsAt = now + 30_000, totalSeconds = 30))
        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.state.value.current).isEqualTo(viewModel.ref(0, 1))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)

        time = now + 30_000
        viewModel.tick()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)
        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus?.ref).isEqualTo(viewModel.ref(0, 1))
        assertThat(viewModel.set(0, 0).done).isTrue()

        // Edited again later, it can run once more
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Time))
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.StartTimer)
    }

    @Test
    fun `Done on a done timed set stops its timer running again`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onMainKey()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Time))
        viewModel.onMainKey()
        assertThat(viewModel.state.value.isTiming(viewModel.ref(0, 0))).isTrue()

        viewModel.onMainKey()

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(alerts.count).isEqualTo(0)
        assertThat(analytics.events.last()).isEqualTo("set_timer_stopped")
    }

    @Test
    fun `a done timed set's weight key is Done and its row shows its tick, as for a set of reps`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onMainKey()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)
        assertThat(viewModel.state.value.startsTimer(viewModel.ref(0, 0))).isFalse()
    }

    @Test
    fun `a done timed set run again offers its timer once more when it's next selected`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onMainKey()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Time))
        viewModel.onMainKey()
        time = now + 30_000
        viewModel.tick()
        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.Done)

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Time))

        assertThat(viewModel.state.value.mainKey).isEqualTo(KeypadMainKey.StartTimer)
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
    fun `moving on to another set stops a running set timer, which its set can start again`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        assertThat(viewModel.state.value.isTiming(viewModel.ref(0, 0))).isTrue()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(viewModel.set(0, 0).done).isFalse()
        assertThat(viewModel.state.value.startsTimer(viewModel.ref(0, 0))).isTrue()
        assertThat(alerts.count).isEqualTo(0)
        assertThat(analytics.events.last()).isEqualTo("set_timer_stopped")
    }

    @Test
    fun `undoing another set stops a running set timer`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onTick(viewModel.ref(1, 0, 0))
        viewModel.onStartSetTimer(viewModel.ref(0, 0))

        viewModel.onTick(viewModel.ref(1, 0, 0))

        assertThat(viewModel.state.value.timer).isNull()
        assertThat(viewModel.set(1, 0, 0).done).isFalse()
    }

    @Test
    fun `a rest keeps running as the selection moves`() {
        val viewModel = viewModel()
        viewModel.onTick(viewModel.ref(0, 0))
        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 0, 0), ValueField.Reps))

        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
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
    fun `picking no exercises changes nothing`() {
        val viewModel = viewModel()
        val saved = store.ongoing

        viewModel.onExercisesPicked(emptyList())

        assertThat(store.ongoing).isSameInstanceAs(saved)
        assertThat(analytics.events).doesNotContain("exercises_added")
    }

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
    fun `no more sets than the limit per exercise`() {
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
    fun `an own exercise renamed meanwhile shows its new name, and is saved`() {
        val viewModel = viewModel()
        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "fly", name = "Cable fly", own = true)))

        assertThat(viewModel.blocks[1].exercises.map { it.exercise?.name }).containsExactly("Cable fly", "raise").inOrder()
        assertThat(viewModel.blocks[0].exercises.first().exercise?.name).isEqualTo("bench")
        assertThat(store.ongoing?.getExerciseGroups()?.get(1)?.exercises?.first()?.exercise?.name).isEqualTo("Cable fly")
    }

    @Test
    fun `own exercises that haven't changed leave the workout as it is`() {
        viewModel()
        store.ongoing = null
        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "fly", name = "fly"), LibraryExercise(uuid = "squat", name = "Squat", own = true)))

        assertThat(store.ongoing).isNull()
    }

    // Undo puts the block back as it was, so it would bring back the old name
    @Test
    fun `a rename ends a deleted set's undo, like any other change`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(0, 0))
        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "bench", name = "Barbell bench press", own = true)))

        assertThat(viewModel.state.value.deletion).isNull()
        assertThat(viewModel.blocks[0].exercises.first().exercise?.name).isEqualTo("Barbell bench press")
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

    // Filling in from history

    @Test
    fun `a new workout's empty weights are filled in from history, as suggestions`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(60f)
        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Weight)).isTrue()
        assertThat(store.ongoing?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.suggestedWeight).isEqualTo(60f)
    }

    @Test
    fun `reps without a target are filled in from history, as suggestions, targets stay as they are`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel(template().apply { routine?.exerciseGroups?.set(0, group("SINGLE", "bench" to 0, sets = 2)) })

        assertThat(viewModel.set(0, 0).reps).isEqualTo(8)
        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Reps)).isTrue()
        assertThat(viewModel.set(1, 0).reps).isEqualTo(12)
        assertThat(viewModel.set(1, 0).isSuggested(ValueField.Reps)).isFalse()
        assertThat(store.ongoing?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.suggestedReps).isEqualTo(8)
    }

    @Test
    fun `a reps suggestion goes once the reps are changed or the set done`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel(template().apply { routine?.exerciseGroups?.set(0, group("SINGLE", "bench" to 0, sets = 2)) })
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("10")
        viewModel.onTick(viewModel.ref(0, 1))

        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Reps)).isFalse()
        assertThat(viewModel.set(0, 1).isSuggested(ValueField.Reps)).isFalse()
    }

    // Set suggestions

    @Test
    fun `a suggested value says it's from last time`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.suggestionNote).isEqualTo(SuggestionNote.LastTime)
    }

    @Test
    fun `a goal's weight says which goal it's for`() {
        settings.goal = MuscleGoal.GROWTH
        prefill.lastWeightKg = 60f
        prefill.oneRepMax = 90f
        prefill.goalWeightKg = 72f
        val viewModel = viewModel()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.suggestionNote).isEqualTo(SuggestionNote.Goal(MuscleGoal.GROWTH, 72f))
    }

    @Test
    fun `with a goal, reps still say they're from last time`() {
        settings.goal = MuscleGoal.GROWTH
        prefill.lastWeightKg = 60f
        prefill.oneRepMax = 90f
        val viewModel = viewModel(template().apply { routine?.exerciseGroups?.set(0, group("SINGLE", "bench" to 0, sets = 2)) })

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))

        assertThat(viewModel.state.value.suggestionNote).isEqualTo(SuggestionNote.LastTime)
    }

    @Test
    fun `a goal without a 1RM for the exercise says the weight is from last time`() {
        settings.goal = MuscleGoal.GROWTH
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.suggestionNote).isEqualTo(SuggestionNote.LastTime)
    }

    @Test
    fun `a value typed over has no note`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("62.5")
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))

        assertThat(viewModel.state.value.suggestionNote).isNull()
    }

    @Test
    fun `a new setting fills the suggested values in again on coming back`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Weight))
        viewModel.type("65")
        viewModel.onHideKeypad()

        settings.goal = MuscleGoal.GROWTH
        prefill.goalWeightKg = 72f
        viewModel.onResume()

        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(72f)
        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Weight)).isTrue()
        // Typed over, so it stays
        assertThat(viewModel.set(0, 1).weightKg).isEqualTo(65f)
        assertThat(store.ongoing?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.getWeightKg()).isEqualTo(72f)
    }

    @Test
    fun `turning suggestions off empties the suggested values on coming back`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        settings.goal = MuscleGoal.NONE
        prefill.fills = false
        viewModel.onResume()

        assertThat(viewModel.set(0, 0).weightKg).isNull()
        // Its target reps came from the template, not history
        assertThat(viewModel.set(0, 0).requiredReps).isEqualTo(8)
    }

    @Test
    fun `coming back with the same setting changes nothing`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        prefill.goalWeightKg = 72f
        viewModel.onResume()

        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(60f)
    }

    @Test
    fun `opening set suggestions logs it and keeps what was typed`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("50")

        viewModel.onSetSuggestionsTapped()

        assertThat(analytics.events).contains("set_suggestions")
        assertThat(viewModel.state.value.typed).isNull()
        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(50f)
    }

    @Test
    fun `history shows as each set's last time`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()

        assertThat(viewModel.state.value.lastTime["bench"]?.map { it.weightKg }).containsExactly(60f, 60f)
    }

    @Test
    fun `a suggestion goes once the weight is changed or the set done`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("62.5")
        viewModel.onTick(viewModel.ref(0, 1))

        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Weight)).isFalse()
        assertThat(viewModel.set(0, 1).isSuggested(ValueField.Weight)).isFalse()
    }

    @Test
    fun `a resumed workout isn't filled in again`() {
        prefill.lastWeightKg = 60f
        val first = viewModel()
        first.onCellTapped(CellFocus(first.ref(0, 0), ValueField.Weight))
        first.onBackspace()
        first.onBackspace()
        first.onHideKeypad()
        store.saved = store.ongoing

        val resumed = viewModel()

        assertThat(resumed.set(0, 0).weightKg).isNull()
        assertThat(resumed.state.value.lastTime).isNotEmpty()
    }

    @Test
    fun `a muscle goal that doesn't fill sets in leaves them, with last time still shown`() {
        prefill.lastWeightKg = 60f
        prefill.fills = false
        val viewModel = viewModel()

        assertThat(viewModel.set(0, 0).weightKg).isNull()
        assertThat(viewModel.state.value.lastTime).isNotEmpty()
    }

    @Test
    fun `an added set is filled in from the last session's set at its place`() {
        prefill.lastWeightKg = 60f
        prefill.lastSets = 3
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Weight))
        viewModel.type("70")
        viewModel.onAddSet(viewModel.blocks[0].id)

        assertThat(viewModel.set(0, 2).weightKg).isEqualTo(60f)
        assertThat(viewModel.set(0, 2).isSuggested(ValueField.Weight)).isTrue()
    }

    @Test
    fun `picked exercises are filled in, the others aren't filled again`() {
        prefill.lastWeightKg = 60f
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.onBackspace()
        viewModel.onBackspace()
        viewModel.onExercisesPicked(listOf(group("SINGLE", "squat" to 5, sets = 1)))

        assertThat(viewModel.set(2, 0).weightKg).isEqualTo(60f)
        assertThat(viewModel.set(0, 0).weightKg).isNull()
    }

    @Test
    fun `without history the sets keep their values`() {
        prefill.fails = true
        val viewModel = viewModel()

        assertThat(viewModel.set(0, 0).weightKg).isNull()
        assertThat(viewModel.state.value.lastTime).isEmpty()
    }

    @Test
    fun `a set changed while history loads keeps its values, the others are filled in`() {
        prefill.lastWeightKg = 60f
        val loading = CompletableDeferred<Unit>()
        prefill.loading = loading
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Weight))
        viewModel.type("40")
        viewModel.onHideKeypad()

        loading.complete(Unit)

        assertThat(viewModel.set(0, 0).weightKg).isEqualTo(40f)
        assertThat(viewModel.set(0, 0).isSuggested(ValueField.Weight)).isFalse()
        assertThat(viewModel.set(0, 1).weightKg).isEqualTo(60f)
    }

    @Test
    fun `history for an exercise deleted while it loads changes nothing`() {
        prefill.lastWeightKg = 60f
        val loading = CompletableDeferred<Unit>()
        prefill.loading = loading
        val viewModel = viewModel()
        viewModel.onDeleteExerciseConfirmed(viewModel.blocks[0].id)

        loading.complete(Unit)

        assertThat(viewModel.blocks).hasSize(1)
        assertThat(viewModel.set(0, 0, 0).weightKg).isEqualTo(60f)
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
    fun `the notification shows a value as it's typed, before the keypad moves on`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")

        assertThat(notification.shown?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.getReps()).isEqualTo(12)
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
    fun `the notification shows the set selected on the screen, and the next one once it's done`() {
        val viewModel = viewModel()
        viewModel.onScreenStarted()
        assertThat(notification.shownSet?.position).isEqualTo(position(0, 0, 0, exercises = 1))

        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 1, 1), ValueField.Weight))
        viewModel.onHideKeypad()
        assertThat(notification.shownSet?.position).isEqualTo(position(1, 1, 1, exercises = 2))

        viewModel.onNotificationMainAction(notification.shownSet!!.position)
        assertThat(viewModel.set(1, 1, 1).done).isTrue()
        assertThat(notification.shownSet?.position).isEqualTo(position(0, 0, 0, exercises = 1))
    }

    @Test
    fun `the notification's main button starts a timed set's timer first, as the screen's does`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("30")
        viewModel.onHideKeypad()
        assertThat(notification.shownSet?.startsTimer).isTrue()

        viewModel.onNotificationMainAction(position(0, 0, 0))
        assertThat(viewModel.state.value.isTiming(viewModel.ref(0, 0))).isTrue()
        assertThat(viewModel.set(0, 0).done).isFalse()
        assertThat(notification.shownSet?.startsTimer).isFalse()

        viewModel.onNotificationMainAction(position(0, 0, 0))
        assertThat(viewModel.set(0, 0).done).isTrue()
    }

    @Test
    fun `a superset's last round with an exercise removed shows the others by their place in the round`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(1, 1, 0))
        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 1, 1), ValueField.Reps))

        assertThat(notification.shownSet?.position).isEqualTo(position(1, 1, 0, exercises = 1))
        viewModel.onNotificationReps(notification.shownSet!!.position, 1)
        assertThat(viewModel.set(1, 1, 1).reps).isEqualTo(13)
    }

    @Test
    fun `the notification's button completes its set, logged as from the notification`() {
        val viewModel = viewModel()
        viewModel.onNotificationMainAction(position(group = 0, set = 0, exercise = 0))

        assertThat(viewModel.set(0, 0).done).isTrue()
        assertThat(viewModel.state.value.timer).isInstanceOf(WorkoutTimer.Rest::class.java)
        assertThat(analytics.events.first()).isEqualTo("set_completed SINGLE notification")
    }

    @Test
    fun `the notification's buttons point at a superset exercise by its place in the round`() {
        val viewModel = viewModel()
        viewModel.onNotificationMainAction(position(group = 1, set = 1, exercise = 1))

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
        assertThat(analytics.events).containsAtLeast("weight_modified 2.5", "reps_modified 7.0").inOrder()
    }

    @Test
    fun `the notification's time ± step it by 10s as the keypad's do, and change a running timer the same`() {
        val viewModel = timedSetRunning(seconds = "30")
        viewModel.onNotificationTime(position(0, 0, 0), 1)
        assertThat(viewModel.set(0, 0).timeSeconds).isEqualTo(40)
        assertThat(viewModel.state.value.timer?.endsAt).isEqualTo(now + 40_000)

        viewModel.onNotificationStopTimer(position(0, 0, 0))
        viewModel.onNotificationTime(position(0, 0, 0), -1)
        assertThat(viewModel.set(0, 0).timeSeconds).isEqualTo(30)
        assertThat(analytics.events.last()).isEqualTo("time_modified 30.0")
    }

    @Test
    fun `a notification button for a set that's gone since does nothing`() {
        val viewModel = viewModel()
        viewModel.onDeleteSet(viewModel.ref(0, 1))
        val saved = store.ongoing

        viewModel.onNotificationMainAction(position(0, 1, 0))
        viewModel.onNotificationReps(position(0, 1, 0), 1)
        viewModel.onNotificationWeight(position(5, 0, 0), 1)

        assertThat(store.ongoing).isSameInstanceAs(saved)
        assertThat(analytics.events.filter { it.startsWith("set_completed") || "modified" in it }).isEmpty()
    }

    @Test
    fun `the notification stops a set timer and a rest, which it logs itself`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)
        viewModel.type("30")
        viewModel.onHideKeypad()

        viewModel.onNotificationMainAction(position(0, 0, 0))
        assertThat(viewModel.state.value.isTiming(viewModel.ref(0, 0))).isTrue()
        viewModel.onNotificationStopTimer(position(0, 0, 0))
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

    private fun FakeStore.savedReps() = ongoing?.getExerciseGroups()?.first()?.exercises?.first()?.sets?.first()?.getReps()

    @Test
    fun `a typed value is saved once the keypad leaves its cell, not at every key`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("6")
        assertThat(store.savedReps()).isEqualTo(8)

        viewModel.onMainKey()

        assertThat(store.savedReps()).isEqualTo(6)
    }

    @Test
    fun `a value being typed is saved when the screen goes away`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("6")

        viewModel.onPause()

        assertThat(store.savedReps()).isEqualTo(6)
        assertThat(viewModel.state.value.typed).isEqualTo("6")
    }

    @Test
    fun `every other change is saved straight away`() {
        val viewModel = viewModel()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.onStep(-1)

        assertThat(store.savedReps()).isEqualTo(7)
    }

    private fun position(group: Int, set: Int, exercise: Int, exercises: Int = 0) =
        ELWorkoutState(groupIndex = group, setIndex = set, exerciseIndex = exercise, exercisesInGroup = exercises)

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

    private class FakeStore : OngoingWorkoutRepository {
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
        var goal = MuscleGoal.HISTORY

        override fun weightUnit() = unit

        override fun weightIncrease() = step

        override fun keepScreenOn() = false

        override fun muscleGoal() = goal
    }

    /**
     * History with every exercise last done at 8 × [lastWeightKg] for [lastSets] sets, filling in empty weights, and reps
     * where there's no target, like the WorkoutPrefillController's history prefill.
     */
    private class FakePrefill : LoadPrefillSourcesUseCase, WorkoutSetPrefill {
        var lastWeightKg: Float? = null
        var lastSets = 2
        var oneRepMax = 0f
        // A 1RM goal's weight, filled in instead of the last weight
        var goalWeightKg: Float? = null
        var fills = true
        var fails = false
        // History arrives once this completes, if set
        var loading: CompletableDeferred<Unit>? = null

        override fun fillsSets() = fills

        override suspend fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>> {
            loading?.await()
            if (fails) return IllegalStateException("offline").left()
            val weight = lastWeightKg ?: return emptyMap<String, PrefillSource>().right()
            return exercises.associate { exercise ->
                val sets = MutableList(lastSets) { ELSet(reps = 8, weight = weight) }
                exercise.uuid!! to PrefillSource(ELExerciseHistory(ELRoutineExercise(exercise.uuid, exercise, sets), ELWorkout()), orm = oneRepMax)
            }.right()
        }

        override fun fill(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet {
            val last = source.lastSession?.exercise?.sets?.getOrNull(index) ?: return set
            val weighed = if (set.weightKg == null) set.copy(weightKg = goalWeightKg ?: last.getWeightKg()) else set
            val empty = set.reps == null && set.timeSeconds == null && set.requiredReps == null && set.requiredTimeSeconds == null
            return if (empty) weighed.copy(reps = last.getReps()) else weighed
        }

        override fun fillAdded(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet {
            val last = source.lastSession?.exercise?.sets?.getOrNull(index) ?: return set
            return set.copy(weightKg = last.getWeightKg(), reps = last.getReps())
        }
    }

    private class FakeNotification : WorkoutLogNotification {
        var started: ELWorkout? = null
        var stopped = false
        // The workout it was last sent, and the set in it to show
        var shown: ELWorkout? = null
        var shownSet: ShownSet? = null
        var rest: Pair<Int, Int>? = null

        override fun start(workout: ELWorkout, shown: ShownSet?) {
            started = workout
            shownSet = shown
        }

        override fun stop() {
            stopped = true
        }

        override fun update(workout: ELWorkout, shown: ShownSet?) {
            this.shown = workout
            shownSet = shown
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

        override fun setSuggestionsOpened() {
            events += "set_suggestions"
        }

        override fun keypadShown(source: KeypadShownFrom) {
            events += "keypad_shown ${source.id}"
        }

        override fun keypadHidden(source: KeypadHiddenBy) {
            events += "keypad_hidden ${source.id}"
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

        override fun valueChanged(field: ValueField, value: Float?) {
            events += "${field.name.lowercase()}_modified $value"
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
