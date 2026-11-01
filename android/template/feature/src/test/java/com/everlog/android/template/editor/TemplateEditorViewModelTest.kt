package com.everlog.android.template.editor

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.session.SessionDefaults
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.everlog.android.template.data.session.LoadTemplateUseCase
import com.everlog.android.template.data.session.SaveTemplateUseCase
import com.everlog.android.template.data.session.TemplateNotFoundException
import com.everlog.android.template.editor.TemplateEditorAnalytics
import com.everlog.android.template.editor.TemplateEditorDialog
import com.everlog.android.template.editor.TemplateEditorExit
import com.everlog.android.template.editor.TemplateEditorMessage
import com.everlog.android.template.editor.TemplateEditorViewModel
import com.everlog.android.template.editor.TemplateLoad
import com.everlog.android.template.editor.TemplateMainKey
import com.everlog.android.template.editor.edited
import com.everlog.android.template.editor.timeForAllSets
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.ValueField
import com.everlog.android.common.session.set
import com.everlog.android.common.sessionui.exercisetable.CellFocus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test

class TemplateEditorViewModelTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val plank = ELExercise(uuid = "plank", name = "Plank")
    private val squat = ELExercise(uuid = "squat", name = "Squat")
    private val load = FakeLoadTemplate()
    private val save = FakeSaveTemplate()
    private val analytics = FakeAnalytics()
    private val ownUpdates = MutableSharedFlow<List<LibraryExercise>>(extraBufferCapacity = 1)

    // Push: bench press 10, 8, 8 reps, then a plank 3 × 0:30, with 1:30 of rest
    private fun push() = ELRoutine(
        uuid = "push",
        name = "Push",
        createdDate = 1L,
        exerciseGroups = mutableListOf(
            group(bench, ELSet(requiredReps = 10), ELSet(requiredReps = 8), ELSet(requiredReps = 8)),
            group(plank, ELSet(requiredTimeSeconds = 30), ELSet(requiredTimeSeconds = 30), ELSet(requiredTimeSeconds = 30)),
        ),
    )

    private fun group(exercise: ELExercise, vararg sets: ELSet) = ELExerciseGroup(
        uuid = "${exercise.uuid}-group",
        type = "SINGLE",
        exercises = mutableListOf(ELRoutineExercise("${exercise.uuid}-item", exercise, sets.toMutableList())),
        restTimeSeconds = 90,
    )

    private fun viewModel(uuid: String? = "push") = TemplateEditorViewModel(
        dispatcher = FakeCoroutineDispatcher,
        uuid = uuid,
        defaultName = "My template",
        loadTemplate = load,
        saveTemplate = save,
        analytics = analytics,
        ownExerciseUpdates = { ownUpdates },
        clock = { 1_000L },
    )

    private fun loaded(): TemplateEditorViewModel {
        load.state.value = Load.Ready(push())
        return viewModel()
    }

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    private val TemplateEditorViewModel.blocks get() = state.value.blocks

    private fun TemplateEditorViewModel.ref(block: Int, round: Int) = SetRef(blocks[block].id, blocks[block].rounds[round].id, 0)

    private fun TemplateEditorViewModel.set(block: Int, round: Int) = blocks.set(ref(block, round))!!

    private fun TemplateEditorViewModel.type(text: String) = text.forEach { onDigit(it) }

    private val saved get() = save.saved.single()

    private fun ELRoutine.targets(group: Int) = exerciseGroups[group].exercises.single().sets.map { it.getRequiredReps() to it.getRequiredTimeSeconds() }

    // Main journeys

    @Test
    fun `a new template opens empty, with the default name and rest`() {
        val viewModel = viewModel(uuid = null)

        val state = viewModel.state.value
        assertThat(state.load).isEqualTo(TemplateLoad.Ready)
        assertThat(state.isNew).isTrue()
        assertThat(state.name).isEqualTo("My template")
        assertThat(state.restSeconds).isEqualTo(ELExerciseGroup.DEFAULT_REST_TIME_SECONDS)
        assertThat(state.blocks).isEmpty()
        assertThat(load.requested).isEmpty()
    }

    @Test
    fun `building a template, picked exercises start at 3 sets of 10, typed targets are saved, and it exits as new`() {
        val viewModel = viewModel(uuid = null)

        viewModel.onExercisesPicked(listOf(ELExerciseGroup(uuid = "g", type = "SINGLE", exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(bench)))))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.type("8")
        viewModel.onSave()

        assertThat(saved.name).isEqualTo("My template")
        assertThat(saved.targets(0)).containsExactly(10 to 0, 8 to 0, 10 to 0).inOrder()
        assertThat(saved.exerciseGroups.single().restTimeSeconds).isEqualTo(ELExerciseGroup.DEFAULT_REST_TIME_SECONDS)
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Saved(saved, isNew = true))
        assertThat(analytics.events).containsAtLeast("exercisesAdded", "saved:new").inOrder()
    }

    @Test
    fun `an existing template loads, then its edits save over it`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.load).isEqualTo(TemplateLoad.Loading)

        load.state.value = Load.Ready(push())
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")
        viewModel.onSave()

        assertThat(load.requested).containsExactly("push")
        assertThat(viewModel.state.value.name).isEqualTo("Push")
        assertThat(saved.uuid).isEqualTo("push")
        assertThat(saved.targets(0)).containsExactly(12 to 0, 8 to 0, 8 to 0).inOrder()
        assertThat(saved.targets(1)).containsExactly(0 to 30, 0 to 30, 0 to 30).inOrder()
        assertThat(saved.exerciseGroups.map { it.restTimeSeconds }).containsExactly(90, 90)
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Saved(saved, isNew = false))
        assertThat(analytics.events).contains("saved:edited")
    }

    @Test
    fun `the main key walks down the sets, across exercises, and closes the keypad after the last`() {
        val viewModel = loaded()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 2), ValueField.Reps))
        assertThat(viewModel.state.value.mainKey()).isEqualTo(TemplateMainKey.NextSet)
        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(1, 0), ValueField.Time))

        viewModel.onMainKey()
        viewModel.onMainKey()
        assertThat(viewModel.state.value.mainKey()).isEqualTo(TemplateMainKey.Done)
        viewModel.onMainKey()
        assertThat(viewModel.state.value.focus).isNull()
    }

    @Test
    fun `the first key replaces the value, more add to it, and backspace takes one off`() {
        val viewModel = loaded()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("1")
        assertThat(viewModel.set(0, 0).reps).isEqualTo(1)
        viewModel.type("5")
        assertThat(viewModel.set(0, 0).reps).isEqualTo(15)
        viewModel.onBackspace()
        assertThat(viewModel.set(0, 0).reps).isEqualTo(1)
    }

    // Options and editing

    @Test
    fun `reps or time switches the whole exercise, and the keypad follows to its time`() {
        val viewModel = loaded()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 1), ValueField.Reps))
        viewModel.onModeSelected(SetMode.Time)

        assertThat((0..2).map { viewModel.set(0, it).mode }).containsExactly(SetMode.Time, SetMode.Time, SetMode.Time)
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 1), ValueField.Time))
        viewModel.onSave()
        assertThat(saved.targets(0)).containsExactly(0 to 30, 0 to 30, 0 to 30)
    }

    @Test
    fun `a time that differs from the exercise's other sets is offered for every set`() {
        val viewModel = loaded()

        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 0), ValueField.Time))
        assertThat(viewModel.state.value.timeForAllSets()).isNull()
        viewModel.type("45")
        assertThat(viewModel.state.value.timeForAllSets()).isEqualTo(45)

        viewModel.onTimeForAllSets()
        assertThat((0..2).map { viewModel.set(1, it).timeSeconds }).containsExactly(45, 45, 45)
        assertThat(viewModel.state.value.timeForAllSets()).isNull()
        assertThat(analytics.events).contains("timeForAllSets:45")
    }

    @Test
    fun `renaming trims the name, and a blank one keeps the old`() {
        val viewModel = loaded()

        viewModel.onNameTapped()
        assertThat(viewModel.state.value.dialog).isEqualTo(TemplateEditorDialog.Name)
        viewModel.onNameSaved("   ")
        assertThat(viewModel.state.value.name).isEqualTo("Push")
        assertThat(viewModel.state.value.hasChanges()).isFalse()

        viewModel.onNameTapped()
        viewModel.onNameSaved("  Push day ")
        assertThat(viewModel.state.value.name).isEqualTo("Push day")
        assertThat(viewModel.state.value.dialog).isNull()
        viewModel.onSave()
        assertThat(saved.name).isEqualTo("Push day")
    }

    @Test
    fun `rest steps by the timer's step, never below one step, for every group`() {
        val viewModel = loaded()
        val step = SessionDefaults.TIME_STEP_SECONDS

        viewModel.onRestTapped()
        viewModel.onRestStep(1)
        assertThat(viewModel.state.value.restSeconds).isEqualTo(90 + step)
        repeat(20) { viewModel.onRestStep(-1) }
        assertThat(viewModel.state.value.restSeconds).isEqualTo(step)

        viewModel.onSave()
        assertThat(saved.exerciseGroups.map { it.restTimeSeconds }).containsExactly(step, step)
    }

    @Test
    fun `a template saved without rest keeps it on minus, and plus starts it at a step`() {
        load.state.value = Load.Ready(push().apply { exerciseGroups.forEach { it.restTimeSeconds = 0 } })
        val viewModel = viewModel()
        val step = SessionDefaults.TIME_STEP_SECONDS

        viewModel.onRestStep(-1)
        assertThat(viewModel.state.value.restSeconds).isEqualTo(0)
        assertThat(viewModel.state.value.hasChanges()).isFalse()
        viewModel.onRestStep(1)
        assertThat(viewModel.state.value.restSeconds).isEqualTo(step)
    }

    @Test
    fun `adding sets stops at the most an exercise can have`() {
        val viewModel = loaded()
        val max = SessionDefaults.MAX_EXERCISE_SETS

        repeat(max) { viewModel.onAddSet(viewModel.blocks[0].id) }

        assertThat(viewModel.blocks[0].rounds).hasSize(max)
        assertThat(viewModel.state.value.message).isEqualTo(TemplateEditorMessage.TooManySets(max))
        assertThat(viewModel.set(0, max - 1).reps).isEqualTo(8)
    }

    @Test
    fun `a deleted set can be undone until something else changes`() {
        val viewModel = loaded()

        viewModel.onDeleteSet(viewModel.ref(0, 1))
        assertThat(viewModel.blocks[0].rounds).hasSize(2)
        viewModel.onUndoDelete()
        assertThat(viewModel.blocks[0].rounds.map { it.sets.single().reps }).containsExactly(10, 8, 8).inOrder()

        viewModel.onDeleteSet(viewModel.ref(0, 1))
        viewModel.onAddSet(viewModel.blocks[1].id)
        assertThat(viewModel.state.value.deletion).isNull()
    }

    @Test
    fun `deleting an exercise's last set deletes the exercise, and undo puts it back where it was`() {
        val viewModel = loaded()

        viewModel.onDeleteSet(viewModel.ref(0, 0))
        assertThat(viewModel.state.value.deletionRemovedExercise()).isFalse()
        viewModel.onDeleteSet(viewModel.ref(0, 0))
        viewModel.onDeleteSet(viewModel.ref(0, 0))

        assertThat(viewModel.blocks.map { it.exercises.single().exercise }).containsExactly(plank)
        assertThat(viewModel.state.value.deletionRemovedExercise()).isTrue()
        viewModel.onUndoDelete()
        assertThat(viewModel.blocks.map { it.exercises.single().exercise }).containsExactly(bench, plank).inOrder()
    }

    @Test
    fun `an exercise joins the next as a superset, and closes the keypad`() {
        val viewModel = loaded()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))

        viewModel.onJoinWithNext(viewModel.blocks[0].id)

        val superset = viewModel.blocks.single()
        assertThat(superset.setType).isEqualTo(ELSetType.SUPER)
        assertThat(superset.exercises.map { it.exercise }).containsExactly(bench, plank).inOrder()
        assertThat(superset.rounds.map { round -> round.sets.map { it.reps to it.timeSeconds } }).containsExactly(
            listOf(10 to null, null to 30), listOf(8 to null, null to 30), listOf(8 to null, null to 30),
        ).inOrder()
        assertThat(viewModel.state.value.focus).isNull()
        assertThat(analytics.events).contains("setType:SUPER")
        assertThat(viewModel.state.value.hasChanges()).isTrue()
    }

    @Test
    fun `a superset joining a third exercise becomes a circuit, and the last exercise has nothing to join`() {
        val viewModel = loaded()
        viewModel.onExercisesPicked(listOf(group(squat, ELSet(requiredReps = 5))))
        val last = viewModel.blocks.last().id

        viewModel.onJoinWithNext(last)
        assertThat(viewModel.blocks).hasSize(3)

        viewModel.onJoinWithNext(viewModel.blocks[0].id)
        viewModel.onJoinWithNext(viewModel.blocks[0].id)
        val circuit = viewModel.blocks.single()
        assertThat(circuit.setType).isEqualTo(ELSetType.CIRCUIT)
        assertThat(circuit.exercises.map { it.exercise }).containsExactly(bench, plank, squat).inOrder()
    }

    @Test
    fun `splitting a superset puts each exercise on its own, in order, with its sets`() {
        val viewModel = loaded()
        viewModel.onJoinWithNext(viewModel.blocks[0].id)

        viewModel.onSplitGroup(viewModel.blocks.single().id)

        assertThat(viewModel.blocks.map { it.setType }).containsExactly(ELSetType.SINGLE, ELSetType.SINGLE)
        assertThat(viewModel.blocks.map { it.exercises.single().exercise }).containsExactly(bench, plank).inOrder()
        assertThat(viewModel.blocks[0].rounds.map { it.sets.single().reps }).containsExactly(10, 8, 8).inOrder()
        assertThat(analytics.events).contains("setType:SINGLE")
        // A single has nothing to split
        viewModel.onSplitGroup(viewModel.blocks[0].id)
        assertThat(viewModel.blocks).hasSize(2)
    }

    @Test
    fun `in a superset, deleting an exercise's last set takes it out of the group`() {
        val viewModel = loaded()
        viewModel.onJoinWithNext(viewModel.blocks[0].id)

        repeat(3) {
            val superset = viewModel.blocks.single()
            viewModel.onDeleteSet(SetRef(superset.id, superset.rounds.first { !it.sets[1].removed }.id, 1))
        }

        assertThat(viewModel.blocks.single().exercises.map { it.exercise }).containsExactly(bench)
        assertThat(viewModel.state.value.deletionRemovedExercise()).isTrue()
    }

    @Test
    fun `dragging closes the keypad, and moving an exercise is a change`() {
        val viewModel = loaded()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))

        viewModel.onDragStarted()
        assertThat(viewModel.state.value.focus).isNull()
        viewModel.onMoveExercise(0, 1)

        assertThat(viewModel.blocks.map { it.exercises.single().exercise }).containsExactly(plank, bench).inOrder()
        assertThat(viewModel.state.value.hasChanges()).isTrue()
        viewModel.onMoveExercise(1, 0)
        assertThat(viewModel.state.value.hasChanges()).isFalse()
    }

    @Test
    fun `the keyboard button opens on the set last typed into, or the first`() {
        val viewModel = loaded()

        viewModel.onShowKeypad()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(0, 0), ValueField.Reps))

        viewModel.onCellTapped(CellFocus(viewModel.ref(1, 2), ValueField.Time))
        viewModel.onHideKeypad()
        viewModel.onShowKeypad()
        assertThat(viewModel.state.value.focus).isEqualTo(CellFocus(viewModel.ref(1, 2), ValueField.Time))
    }

    @Test
    fun `a typed value is logged once, as typing moves on, and a stepped one straight away`() {
        val viewModel = loaded()

        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")
        assertThat(analytics.events.filter { it.startsWith("value") }).isEmpty()
        viewModel.onMainKey()
        viewModel.onStep(1)
        viewModel.onMainKey()
        viewModel.onMainKey()
        viewModel.type("45")
        viewModel.onHideKeypad()

        assertThat(analytics.events.filter { it.startsWith("value") })
            .containsExactly("value:Reps:12", "value:Reps:9", "value:Time:45").inOrder()
    }

    // Leaving

    @Test
    fun `leaving with changes asks, and discarding closes without saving`() {
        val viewModel = loaded()
        viewModel.onAddSet(viewModel.blocks[0].id)

        viewModel.onClose()
        assertThat(viewModel.state.value.dialog).isEqualTo(TemplateEditorDialog.Leave)
        viewModel.onDialogDismissed()
        assertThat(viewModel.state.value.exit).isNull()

        viewModel.onBack()
        viewModel.onDiscard()
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
        assertThat(analytics.events.filter { it.startsWith("leave") })
            .containsExactly("leave:shown", "leave:cancelled", "leave:shown", "leave:discarded").inOrder()
    }

    @Test
    fun `saving from the prompt on leaving saves the template, and says it was picked there`() {
        val viewModel = loaded()
        viewModel.onAddSet(viewModel.blocks[0].id)

        viewModel.onClose()
        viewModel.onSave()

        assertThat(save.saved).hasSize(1)
        assertThat(analytics.events).containsAtLeast("leave:shown", "leave:saved", "saved:edited").inOrder()
    }

    @Test
    fun `a new template left untouched closes without asking or saving`() {
        val viewModel = viewModel(uuid = null)

        viewModel.onBack()

        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
        assertThat(analytics.events.filter { it.startsWith("leave") }).isEmpty()
    }

    @Test
    fun `edits undone by hand leave nothing to ask about`() {
        val viewModel = loaded()

        viewModel.onAddSet(viewModel.blocks[0].id)
        viewModel.onDeleteSet(viewModel.ref(0, 3))
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        viewModel.type("12")
        assertThat(viewModel.state.value.hasChanges()).isTrue()
        viewModel.onBackspace()
        viewModel.onBackspace()
        viewModel.type("10")
        viewModel.onRestTapped()
        viewModel.onRestStep(1)
        viewModel.onRestStep(-1)
        viewModel.onDialogDismissed()

        assertThat(viewModel.state.value.hasChanges()).isFalse()
        viewModel.onClose()
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Closed)
    }

    @Test
    fun `back closes the keypad first, then leaves without asking when nothing changed`() {
        val viewModel = loaded()
        viewModel.onShowKeypad()

        viewModel.onBack()
        assertThat(viewModel.state.value.focus).isNull()
        viewModel.onBack()
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Closed)
    }

    @Test
    fun `saving an unchanged template just closes`() {
        val viewModel = loaded()

        viewModel.onSave()

        assertThat(save.saved).isEmpty()
        assertThat(viewModel.state.value.exit).isEqualTo(TemplateEditorExit.Closed)
    }

    @Test
    fun `an own exercise renamed while the template is open shows its new name, and is saved with it`() {
        val viewModel = loaded()

        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "plank", name = "Side plank", own = true)))

        assertThat(viewModel.blocks.map { it.exercises.single().exercise?.name }).containsExactly("Bench press", "Side plank").inOrder()
        assertThat(viewModel.state.value.hasChanges()).isFalse()
        viewModel.onAddSet(viewModel.blocks[0].id)
        viewModel.onSave()
        assertThat(saved.exerciseGroups[1].exercises.single().exercise?.name).isEqualTo("Side plank")
    }

    @Test
    fun `a rename arriving before the template has loaded doesn't stop it loading`() {
        val viewModel = viewModel()

        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "plank", name = "Side plank", own = true)))
        load.state.value = Load.Ready(push())

        assertThat(viewModel.state.value.load).isEqualTo(TemplateLoad.Ready)
        assertThat(viewModel.blocks).hasSize(2)
    }

    // Edge cases

    @Test
    fun `a template deleted meanwhile says so`() {
        load.state.value = Load.Unavailable(TemplateNotFoundException())

        assertThat(viewModel().state.value.load).isEqualTo(TemplateLoad.NotFound)
    }

    @Test
    fun `a template that can't be read can be tried again`() {
        load.state.value = Load.Unavailable(IllegalStateException("offline"))
        val viewModel = viewModel()
        assertThat(viewModel.state.value.load).isEqualTo(TemplateLoad.Failed)

        load.state.value = Load.Ready(push())
        viewModel.onRetry()

        assertThat(viewModel.state.value.load).isEqualTo(TemplateLoad.Ready)
        assertThat(load.requested).containsExactly("push", "push")
    }

    @Test
    fun `a template that changes elsewhere while it's open keeps the edits`() {
        val viewModel = loaded()
        viewModel.onNameTapped()
        viewModel.onNameSaved("Mine")

        load.state.value = Load.Ready(push().apply { name = "Theirs" })

        assertThat(viewModel.state.value.name).isEqualTo("Mine")
    }

    @Test
    fun `a failed save says so and stays, so it can be saved again`() {
        val viewModel = loaded()
        viewModel.onAddSet(viewModel.blocks[0].id)
        save.result = Either.Left(IllegalStateException("No user"))

        viewModel.onSave()

        assertThat(viewModel.state.value.message).isEqualTo(TemplateEditorMessage.SaveFailed)
        assertThat(viewModel.state.value.exit).isNull()
        assertThat(viewModel.state.value.saving).isFalse()
        assertThat(analytics.events).doesNotContain("saved:edited")
    }

    @Test
    fun `a template saved without a name gets the default one`() {
        load.state.value = Load.Ready(push().apply { name = null })

        assertThat(viewModel().state.value.name).isEqualTo("My template")
    }

    @Test
    fun `the decimal point is ignored, as templates hold whole reps and times`() {
        val viewModel = loaded()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))

        viewModel.type("1")
        viewModel.onDecimal()
        viewModel.type("2")

        assertThat(viewModel.set(0, 0).reps).isEqualTo(12)
    }

    private class FakeLoadTemplate : LoadTemplateUseCase {
        val state = MutableStateFlow<Load<ELRoutine>>(Load.Loading)
        val requested = mutableListOf<String>()

        override fun invoke(uuid: String): Flow<Load<ELRoutine>> {
            requested += uuid
            return state
        }
    }

    private class FakeSaveTemplate : SaveTemplateUseCase {
        var result: Either<Throwable, Unit> = Either.Right(Unit)
        val saved = mutableListOf<ELRoutine>()

        override suspend fun invoke(routine: ELRoutine): Either<Throwable, Unit> = result.onRight { saved += routine }
    }

    private class FakeAnalytics : TemplateEditorAnalytics {
        val events = mutableListOf<String>()

        override fun screenViewed() {
            events += "screen"
        }

        override fun templateSaved(isNew: Boolean) {
            events += if (isNew) "saved:new" else "saved:edited"
        }

        override fun exercisesAdded() {
            events += "exercisesAdded"
        }

        override fun setAdded() {
            events += "setAdded"
        }

        override fun setDeleted() {
            events += "setDeleted"
        }

        override fun restChanged(seconds: Int) {
            events += "rest:$seconds"
        }

        override fun timeForAllSets(seconds: Int) {
            events += "timeForAllSets:$seconds"
        }

        override fun setTypeChanged(type: ELSetType) {
            events += "setType:${type.name}"
        }

        override fun valueChanged(field: ValueField, value: Int?) {
            events += "value:${field.name}:$value"
        }

        override fun leavePromptShown() {
            events += "leave:shown"
        }

        override fun leavePromptSaved() {
            events += "leave:saved"
        }

        override fun leavePromptDiscarded() {
            events += "leave:discarded"
        }

        override fun leavePromptCancelled() {
            events += "leave:cancelled"
        }
    }
}
