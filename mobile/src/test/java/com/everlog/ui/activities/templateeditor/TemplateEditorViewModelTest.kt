package com.everlog.ui.activities.templateeditor

import arrow.core.Either
import com.everlog.config.AppConfig
import com.everlog.data.controllers.templatesession.LoadTemplateUseCase
import com.everlog.data.controllers.templatesession.SaveTemplateUseCase
import com.everlog.data.controllers.templatesession.TemplateNotFoundException
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.sync.Load
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.ui.components.exercisetable.CellFocus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test

class TemplateEditorViewModelTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val plank = ELExercise(uuid = "plank", name = "Plank")
    private val load = FakeLoadTemplate()
    private val save = FakeSaveTemplate()
    private val analytics = FakeAnalytics()

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
        assertThat(state.restSeconds).isEqualTo(AppConfig.configuration.defaultRestTimeSeconds)
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
        assertThat(saved.exerciseGroups.single().restTimeSeconds).isEqualTo(AppConfig.configuration.defaultRestTimeSeconds)
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
        assertThat(viewModel.state.value.changed).isFalse()

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
        val step = AppConfig.configuration.defaultRestTimeOffsetSeconds

        viewModel.onRestTapped()
        viewModel.onRestStep(1)
        assertThat(viewModel.state.value.restSeconds).isEqualTo(90 + step)
        repeat(20) { viewModel.onRestStep(-1) }
        assertThat(viewModel.state.value.restSeconds).isEqualTo(step)

        viewModel.onSave()
        assertThat(saved.exerciseGroups.map { it.restTimeSeconds }).containsExactly(step, step)
    }

    @Test
    fun `adding sets stops at the most an exercise can have`() {
        val viewModel = loaded()
        val max = AppConfig.configuration.maxExerciseSets

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
    fun `deleting an exercise asks first, and takes the keypad's cell with it`() {
        val viewModel = loaded()
        viewModel.onCellTapped(CellFocus(viewModel.ref(0, 0), ValueField.Reps))
        val bench = viewModel.blocks[0].id

        viewModel.onDeleteExercise(bench)
        assertThat(viewModel.state.value.dialog).isEqualTo(TemplateEditorDialog.DeleteExercise(bench))
        viewModel.onDeleteExerciseConfirmed(bench)

        assertThat(viewModel.blocks.map { it.exercises.single().exercise }).containsExactly(plank)
        assertThat(viewModel.state.value.focus).isNull()
        assertThat(viewModel.state.value.selected).isNull()
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
    }
}
