package com.everlog.ui.activities.templateeditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.core.Either
import com.everlog.config.AppConfig
import com.everlog.data.controllers.templatesession.LoadTemplateUseCase
import com.everlog.data.controllers.templatesession.SaveTemplateUseCase
import com.everlog.data.controllers.templatesession.TemplateNotFoundException
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.sync.Load
import com.everlog.ui.components.exercisetable.ExerciseTab
import com.everlog.ui.components.exercisetable.ExerciseTableTags
import com.everlog.ui.design.theme.Theme
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The template editor driven as a user would, through its table, keypad and dialogs, with the real view-model. A few
 * high level journeys rather than a test per detail (the view-model's unit tests cover those), as each one is slow:
 * building a template, editing one with a superset, the ways of leaving, and loading or saving that fails.
 * Loading and saving are fakes here; TemplateEditorActivityTest goes through the real activity and Firestore.
 */
@RunWith(AndroidJUnit4::class)
class TemplateEditorScenarioTest {

    val compose = createComposeRule()

    // The longest journeys take a couple of minutes on a slow emulator
    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(4, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val load = FakeLoadTemplate()
    private val save = FakeSaveTemplate()
    private val ownUpdates = MutableSharedFlow<List<LibraryExercise>>(extraBufferCapacity = 1)
    private var exit: TemplateEditorExit? = null
    // The exercise whose details were opened, and on which tab
    private var opened: Pair<String?, ExerciseTab>? = null

    @Test
    fun buildingANewTemplate() {
        val viewModel = viewModel(uuid = null)
        var picks = listOf(picked("Squat"))
        show(viewModel, onAddExercise = { viewModel.onExercisesPicked(picks) })

        // Empty: the default name and rest, a faded example, and Add exercise as the keypad's only action
        compose.onNodeWithText("New template").assertExists()
        compose.onNodeWithText("My template").assertExists()
        compose.onNodeWithText("1:00").assertExists()
        compose.onNodeWithText("Add the exercises you want in this template").assertExists()
        compose.onNodeWithText("EXAMPLE · NOT YOUR DATA").assertExists()
        compose.onNode(hasContentDescription("Show keypad")).assertDoesNotExist()
        key("Add exercise").performClick()

        // A picked exercise starts at 3 sets of 10, and the example goes
        compose.onNodeWithText("EXAMPLE · NOT YOUR DATA").assertDoesNotExist()
        (0..2).forEach { cell(0, it).assertTextEquals("10") }
        picks = listOf(picked("Plank"))
        key("Add exercise").performClick()

        // Squat set 2 typed over: the first key replaces, backspace takes one off, + steps up a rep
        cell(0, 1).performClick()
        compose.onNodeWithText("SET 2 OF 3 · REPS").assertExists()
        keys("12")
        cell(0, 1).assertTextEquals("12")
        backspace()
        keys("5")
        key("+").performClick()
        cell(0, 1).assertTextEquals("16")

        // Next set walks down the squat's sets and on to the plank's
        key("Next set").performClick()
        compose.onNodeWithText("SET 3 OF 3 · REPS").assertExists()
        key("Next set").performClick()
        compose.onNodeWithText("SET 1 OF 3 · REPS").assertExists()
        key("Plank").assertExists()

        // Time switches the whole plank to 0:30 sets; 1:15 typed on one is offered for every set
        key("Time").performClick()
        (0..2).forEach { cell(1, it).assertTextEquals("0:30") }
        keys("115")
        cell(1, 0).assertTextEquals("1:15")
        compose.onNodeWithText("Use 1:15 for every set").assertExists()
        compose.onNode(hasText("Apply") and hasAnyAncestor(hasTestTag(TemplateEditorTags.TimeForAllSets))).performClick()
        (0..2).forEach { cell(1, it).assertTextEquals("1:15") }
        compose.onNodeWithTag(TemplateEditorTags.TimeForAllSets).assertDoesNotExist()

        // On the last set the main key is Done, which closes the keypad; the keyboard button opens it there again
        key("Next set").performClick()
        key("Next set").performClick()
        key("Done").performClick()
        compose.onNodeWithText("SET 3 OF 3 · TIME").assertDoesNotExist()
        compose.onNode(hasContentDescription("Show keypad")).performClick()
        compose.onNodeWithText("SET 3 OF 3 · TIME").assertExists()
        key("Done").performClick()

        // Add set copies the last set
        addSet(0)
        cell(0, 3).assertTextEquals("10")

        // A blank name keeps the old one; a new one is trimmed
        rename("   ")
        compose.onNodeWithText("My template").assertExists()
        rename("  Legs ")
        compose.onNodeWithText("Legs").assertExists()

        // Rest steps by 10s and never below 10s
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToIndex(0)
        compose.onNodeWithTag(TemplateEditorTags.Rest).performClick()
        repeat(7) { compose.onNode(hasContentDescription("Less rest")).performClick() }
        compose.onNode(hasText("0:10") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasContentDescription("More rest")).performClick()
        compose.onNode(hasText("Done") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("0:20").assertExists()

        // Saved as a new template with everything above
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { exit != null }
        val saved = save.saved.single()
        assertThat(exit).isEqualTo(TemplateEditorExit.Saved(saved, isNew = true))
        assertThat(saved.name).isEqualTo("Legs")
        assertThat(saved.exerciseGroups.map { it.restTimeSeconds }).containsExactly(20, 20)
        assertThat(saved.targets(0)).containsExactly(10 to 0, 16 to 0, 10 to 0, 10 to 0).inOrder()
        assertThat(saved.targets(1)).containsExactly(0 to 75, 0 to 75, 0 to 75).inOrder()
    }

    @Test
    fun editingATemplateWithASuperset() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))
        compose.onNodeWithText("Edit template").assertExists()
        compose.onNodeWithText("Push").assertExists()
        compose.onNodeWithText("1:30").assertExists()

        // The superset is one card with rows 1a, 1b, round by round, under its type
        showExercise(1)
        compose.onNodeWithText("SUPER SET").assertExists()
        listOf("1a", "1b", "2a", "2b").forEach { compose.onNodeWithText(it).assertExists() }
        cell(1, 0, 1).performClick()
        compose.onNodeWithText("SET 1 OF 2 · 2 OF 2 · REPS").assertExists()
        keys("15")
        cell(1, 0, 1).assertTextEquals("15")

        // Back closes the keypad first
        Espresso.pressBack()
        compose.onNodeWithText("SET 1 OF 2 · 2 OF 2 · REPS").assertDoesNotExist()
        assertThat(exit).isNull()

        // A set swiped away comes back with Undo
        row(0, 1).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(ExerciseTableTags.delete(ExerciseTableTags.set(0, 1, 0))).performClick()
        cell(0, 2).assertDoesNotExist()
        compose.onNodeWithText("Undo").performClick()
        cell(0, 2).assertTextEquals("8")

        // Adding sets stops at the most an exercise can have, and says so
        val max = AppConfig.configuration.maxExerciseSets
        repeat(max - 2) { addSet(0) }
        cell(0, max - 1).assertTextEquals("8")
        compose.onNodeWithText("You cannot add more than $max sets per exercise").assertExists()

        // The menu opens an exercise's history; its Delete asks first, and Cancel keeps the exercise
        showExercise(0)
        compose.onNodeWithTag(ExerciseTableTags.menu(0)).performClick()
        compose.onNode(hasText("History") and hasAnyAncestor(isPopup())).performClick()
        assertThat(opened).isEqualTo("Bench press" to ExerciseTab.History)
        showExercise(1)
        compose.onNodeWithTag(ExerciseTableTags.menu(1)).performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isPopup())).performClick()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("SUPER SET").assertExists()

        // One of the user's own exercises renamed elsewhere shows its new name straight away
        ownUpdates.tryEmit(listOf(LibraryExercise(uuid = "bench", name = "Paused bench press", own = true)))
        showExercise(0)
        compose.onNodeWithText("Paused bench press").assertExists()

        // Close asks about the changes; Keep editing stays, and Save from the prompt saves them
        compose.onNode(hasContentDescription("Close")).performClick()
        compose.onNode(hasText("Keep editing") and hasAnyAncestor(isDialog())).performClick()
        assertThat(exit).isNull()
        Espresso.pressBack()
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        val saved = save.saved.single()
        assertThat(exit).isEqualTo(TemplateEditorExit.Saved(saved, isNew = false))
        assertThat(saved.uuid).isEqualTo("push")
        assertThat(saved.exerciseGroups[0].exercises.single().exercise?.name).isEqualTo("Paused bench press")
        assertThat(saved.targets(0)).hasSize(max)
        assertThat(saved.exerciseGroups[1].type).isEqualTo(ELSetType.SUPER.name)
        assertThat(saved.exerciseGroups[1].exercises.map { exercise -> exercise.sets.map { it.getRequiredReps() } })
            .containsExactly(listOf(12, 12), listOf(15, 12)).inOrder()
    }

    @Test
    fun reorganisingWithTheEditList() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))

        // The edit list takes the table's place; Back leaves it
        compose.onNode(hasContentDescription("Edit list")).performClick()
        compose.onNodeWithTag(ExerciseTableTags.EditList).assertExists()
        compose.onNodeWithText("Drag the grip to move an exercise. Tick exercises to group them, change their set type or delete them.").assertExists()
        Espresso.pressBack()
        compose.onNodeWithTag(ExerciseTableTags.EditList).assertDoesNotExist()
        assertThat(exit).isNull()
        compose.onNode(hasContentDescription("Edit list")).performClick()

        // Bench press dragged below the superset
        compose.onNodeWithTag(ExerciseTableTags.editGrip(0)).performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 6f), delayMillis = 200)
            up()
        }
        compose.onNode(hasText("Cable fly") and hasAnyAncestor(hasTestTag(ExerciseTableTags.editCard(0)))).assertExists()

        // Raise taken out of the superset: the fly is left as a single, the raise follows it
        pick(0, 1)
        editAction("Ungroup").performClick()
        compose.onNode(hasText("SUPER SET")).assertDoesNotExist()
        compose.onNode(hasText("Lateral raise") and hasAnyAncestor(hasTestTag(ExerciseTableTags.editCard(1)))).assertExists()

        // One ticked offers its set type; the sheet only lets the ones for one exercise be picked
        pick(2, 0)
        editAction("Set type").performClick()
        compose.onNodeWithText("Set type for 1 exercise").assertExists()
        compose.onNode(hasText("Super Set") and hasAnyAncestor(hasTestTag(TemplateEditorTags.SetTypeSheet))).performClick()
        compose.onNodeWithTag(TemplateEditorTags.SetTypeSheet).assertExists()
        compose.onNode(hasText("Drop Set") and hasAnyAncestor(hasTestTag(TemplateEditorTags.SetTypeSheet))).performClick()
        compose.waitUntil(5_000L) { compose.onAllNodes(hasTestTag(TemplateEditorTags.SetTypeSheet)).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("DROP SET").assertDoesNotExist()

        // Fly and bench press grouped as a superset where the fly was
        pick(0, 0)
        pick(2, 0)
        editAction("Group 2 exercises").performClick()
        compose.onNode(hasText("Super Set") and hasAnyAncestor(hasTestTag(TemplateEditorTags.SetTypeSheet))).performClick()
        compose.waitUntil(5_000L) { compose.onAllNodes(hasText("SUPER SET")).fetchSemanticsNodes().isNotEmpty() }

        // The raise deleted once confirmed, then back to the table
        pick(1, 0)
        editAction("Delete").performClick()
        compose.onNodeWithText("Delete 1 exercise?").assertExists()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Lateral raise").assertDoesNotExist()
        editAction("Done").performClick()
        compose.onNodeWithTag(ExerciseTableTags.EditList).assertDoesNotExist()
        listOf("1a", "1b").forEach { compose.onNodeWithText(it).assertExists() }

        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { exit != null }
        val saved = save.saved.single()
        assertThat(saved.exerciseGroups.map { group -> group.type to group.exercises.map { it.exercise?.name } })
            .containsExactly("SUPER" to listOf("Cable fly", "Bench press"))
        assertThat(saved.exerciseGroups.single().exercises.map { it.sets.size }).containsExactly(3, 3)
    }

    @Test
    fun deletingAnExercise() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))

        showExercise(0)
        compose.onNodeWithTag(ExerciseTableTags.menu(0)).performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Delete Bench press?").assertExists()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()

        compose.onNodeWithText("Bench press").assertDoesNotExist()
        compose.onNodeWithText("SUPER SET").assertExists()
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(save.saved.single().exerciseGroups.map { it.type }).containsExactly(ELSetType.SUPER.name)
    }

    @Test
    fun anUntouchedNewTemplateClosesWithoutAsking() {
        show(viewModel(uuid = null))
        compose.onNodeWithText("New template").assertExists()

        Espresso.pressBack()

        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
    }

    @Test
    fun editsUndoneByHandCloseWithoutAsking() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))

        // A set added and swiped away again, and a value typed back to what it was
        addSet(0)
        row(0, 3).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(ExerciseTableTags.delete(ExerciseTableTags.set(0, 3, 0))).performClick()
        cell(0, 0).performClick()
        keys("9")
        backspace()
        keys("10")
        key("Next set").performClick()

        compose.onNode(hasContentDescription("Close")).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
    }

    @Test
    fun discardingChanges() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))
        addSet(0)

        Espresso.pressBack()
        compose.onNodeWithText("Save your changes?").assertExists()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
    }

    @Test
    fun aTemplateThatCantBeLoadedIsTriedAgain() {
        load.state.value = Load.Unavailable(IllegalStateException("offline"))
        show(viewModel(uuid = "push"))

        // Nothing to save or type into until it loads
        compose.onNodeWithText("Couldn't load this template.").assertExists()
        compose.onNodeWithTag(TemplateEditorTags.Save).assertDoesNotExist()
        compose.onNodeWithTag(TemplateEditorTags.Keypad).assertDoesNotExist()
        load.state.value = Load.Ready(push())
        compose.onNodeWithText("Try again").performClick()

        compose.onNodeWithText("Push").assertExists()
        compose.onNodeWithTag(TemplateEditorTags.Save).assertExists()
        cell(0, 0).assertTextEquals("10")
    }

    @Test
    fun aDeletedTemplateSaysSoAndCloses() {
        load.state.value = Load.Unavailable(TemplateNotFoundException())
        show(viewModel(uuid = "push"))

        compose.onNodeWithText("This template has been deleted.").assertExists()
        compose.onNodeWithText("Try again").assertDoesNotExist()
        compose.onNode(hasContentDescription("Close")).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(exit).isEqualTo(TemplateEditorExit.Closed)
    }

    @Test
    fun aFailedSaveSaysSoAndCanBeSavedAgain() {
        load.state.value = Load.Ready(push())
        show(viewModel(uuid = "push"))
        addSet(0)
        save.result = Either.Left(IllegalStateException("No user"))

        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText("Couldn't save the template. Try again.")).fetchSemanticsNodes().isNotEmpty() }
        assertThat(exit).isNull()

        save.result = Either.Right(Unit)
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(save.saved.single().targets(0)).hasSize(4)
    }

    private fun viewModel(uuid: String?) = TemplateEditorViewModel(
        dispatcher = Dispatchers.Main,
        uuid = uuid,
        defaultName = "My template",
        loadTemplate = load,
        saveTemplate = save,
        analytics = NoAnalytics,
        ownExerciseUpdates = { ownUpdates },
    )

    private fun show(viewModel: TemplateEditorViewModel, onAddExercise: () -> Unit = {}) = compose.setContent {
        Theme {
            TemplateEditorScreen(
                viewModel = viewModel,
                onAddExercise = onAddExercise,
                onOpenExercise = { exercise, tab -> opened = exercise.name to tab },
                onExit = { exit = it },
            )
        }
    }

    // Bench press 10, 8 and 8 reps, then a superset of fly and raise, 2 rounds of 12, with 1:30 of rest
    private fun push() = ELRoutine(
        uuid = "push",
        name = "Push",
        exerciseGroups = mutableListOf(
            group(ELSetType.SINGLE, ELExercise(uuid = "bench", name = "Bench press") to listOf(10, 8, 8)),
            group(
                ELSetType.SUPER,
                ELExercise(uuid = "fly", name = "Cable fly") to listOf(12, 12),
                ELExercise(uuid = "raise", name = "Lateral raise") to listOf(12, 12),
            ),
        ),
    )

    private fun group(type: ELSetType, vararg exercises: Pair<ELExercise, List<Int>>) = ELExerciseGroup(
        uuid = "group-${exercises.first().first.uuid}",
        type = type.name,
        restTimeSeconds = 90,
        exercises = exercises.map { (exercise, reps) ->
            ELRoutineExercise("${exercise.uuid}-item", exercise, reps.map { ELSet(requiredReps = it) }.toMutableList())
        }.toMutableList(),
    )

    // As the picker hands an exercise over: one empty set
    private fun picked(name: String) = ELExerciseGroup(
        uuid = "group-$name",
        type = ELSetType.SINGLE.name,
        exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(ELExercise(uuid = name, name = name))),
    )

    private fun ELRoutine.targets(group: Int) =
        exerciseGroups[group].exercises.single().sets.map { it.getRequiredReps() to it.getRequiredTimeSeconds() }

    // The table's first item is the name and rest, so an exercise is one further down
    private fun showExercise(block: Int) {
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToIndex(block + 1)
    }

    // Scrolled into view, as a long exercise runs past the screen and taps off it don't land
    private fun inView(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun row(block: Int, round: Int, exercise: Int = 0) = inView(ExerciseTableTags.set(block, round, exercise))

    private fun cell(block: Int, round: Int, exercise: Int = 0): SemanticsNodeInteraction {
        showExercise(block)
        val tag = ExerciseTableTags.count(ExerciseTableTags.set(block, round, exercise))
        return if (compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithTag(tag) else inView(tag)
    }

    // By its click action: scrolled into view, it can still sit under the keypad, which takes a tap there
    private fun addSet(block: Int) = inView(ExerciseTableTags.addSet(block)).performSemanticsAction(SemanticsActions.OnClick)

    private fun rename(name: String) {
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToIndex(0)
        compose.onNodeWithTag(TemplateEditorTags.Name).performClick()
        compose.onNodeWithTag(TemplateEditorTags.NameField).performTextReplacement(name)
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun pick(block: Int, exercise: Int) = compose.onNodeWithTag(ExerciseTableTags.editPick(block, exercise)).performClick()

    private fun editAction(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(TemplateEditorTags.EditFooter)))

    // A key or button on the docked keypad, rather than the same text in the table
    private fun key(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(TemplateEditorTags.Keypad)))

    private fun keys(digits: String) = digits.forEach { key(it.toString()).performClick() }

    private fun backspace() =
        compose.onNode(hasContentDescription("Delete") and hasAnyAncestor(hasTestTag(TemplateEditorTags.Keypad))).performClick()

    private class FakeLoadTemplate : LoadTemplateUseCase {
        val state = MutableStateFlow<Load<ELRoutine>>(Load.Loading)

        override fun invoke(uuid: String): Flow<Load<ELRoutine>> = state
    }

    private class FakeSaveTemplate : SaveTemplateUseCase {
        var result: Either<Throwable, Unit> = Either.Right(Unit)
        val saved = mutableListOf<ELRoutine>()

        override suspend fun invoke(routine: ELRoutine): Either<Throwable, Unit> = result.onRight { saved += routine }
    }

    private object NoAnalytics : TemplateEditorAnalytics {
        override fun screenViewed() = Unit
        override fun templateSaved(isNew: Boolean) = Unit
        override fun exercisesAdded() = Unit
        override fun setAdded() = Unit
        override fun setDeleted() = Unit
        override fun restChanged(seconds: Int) = Unit
        override fun timeForAllSets(seconds: Int) = Unit
        override fun setTypePicked(type: ELSetType) = Unit
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
