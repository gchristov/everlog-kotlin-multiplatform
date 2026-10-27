package com.everlog.ui.activities.templateeditor

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.core.Either
import com.everlog.data.controllers.templatesession.LoadTemplateUseCase
import com.everlog.data.controllers.templatesession.SaveTemplateUseCase
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.sync.Load
import com.everlog.ui.components.exercisetable.ExerciseTableTags
import com.everlog.ui.design.theme.Theme
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The template editor driven as a user would, through its table and keypad, with the real view-model: building a
 * template, editing one and leaving, and loading one that fails. The view-model's unit tests cover the details.
 * Loading and saving are fakes here; TemplateEditorActivityTest goes through Firestore.
 */
@RunWith(AndroidJUnit4::class)
class TemplateEditorScenarioTest {

    val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val load = FakeLoadTemplate()
    private val save = FakeSaveTemplate()
    private var exit: TemplateEditorExit? = null

    @Test
    fun buildingANewTemplate() {
        val viewModel = viewModel(uuid = null)
        show(viewModel, onAddExercise = { viewModel.onExercisesPicked(listOf(picked("Plank"))) })

        // Empty, with a hint and Add exercise as the keypad's only action
        compose.onNodeWithText("Add the exercises you want in this template").assertExists()
        key("Add exercise").performClick()

        // A picked exercise starts at 3 sets of 10
        (0..2).forEach { cell(0, it).assertTextEquals("10") }

        // Set 2 is typed over, and Next set moves to set 3
        cell(0, 1).performClick()
        keys("8")
        cell(0, 1).assertTextEquals("8")
        key("Next set").performClick()
        compose.onNodeWithText("SET 3 OF 3 · REPS").assertExists()

        // Time switches the whole exercise to 0:30 sets; one typed to 0:45 is offered for every set
        key("Time").performClick()
        (0..2).forEach { cell(0, it).assertTextEquals("0:30") }
        keys("45")
        compose.onNodeWithText("Use 0:45 for every set").assertExists()
        compose.onNode(hasText("Apply") and hasAnyAncestor(hasTestTag(TemplateEditorTags.TimeForAllSets))).performClick()
        (0..2).forEach { cell(0, it).assertTextEquals("0:45") }
        key("Done").performClick()

        // Renamed in its dialog, then saved
        compose.onNodeWithTag(TemplateEditorTags.Name).performClick()
        compose.onNodeWithTag(TemplateEditorTags.NameField).performTextReplacement("Core")
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Core").assertExists()
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        val saved = save.saved.single()
        assertThat(exit).isEqualTo(TemplateEditorExit.Saved(saved, isNew = true))
        assertThat(saved.name).isEqualTo("Core")
        assertThat(saved.exerciseGroups.single().exercises.single().sets.map { it.getRequiredTimeSeconds() }).containsExactly(45, 45, 45)
    }

    @Test
    fun editingATemplateAndLeaving() {
        load.state.value = Load.Ready(push())
        val viewModel = viewModel(uuid = "push")
        show(viewModel)
        compose.onNodeWithText("Push").assertExists()

        // A set added, one deleted and brought back
        compose.onNodeWithText("Add set").performClick()
        cell(0, 3).assertTextEquals("8")
        compose.onNodeWithTag(ExerciseTableTags.set(0, 1, 0)).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(ExerciseTableTags.delete(ExerciseTableTags.set(0, 1, 0))).performClick()
        cell(0, 3).assertDoesNotExist()
        compose.onNodeWithText("Undo").performClick()
        cell(0, 1).assertTextEquals("8")

        // Rest goes up a step from its dialog
        compose.onNodeWithTag(TemplateEditorTags.Rest).performClick()
        compose.onNodeWithContentDescription("More rest").performClick()
        compose.onNode(hasText("Done") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("1:40").assertExists()

        // Close asks about the changes; Keep editing stays, Discard leaves without saving
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNode(hasText("Keep editing") and hasAnyAncestor(isDialog())).performClick()
        assertThat(exit).isNull()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(TIMEOUT) { exit != null }
        assertThat(exit).isEqualTo(TemplateEditorExit.Closed)
        assertThat(save.saved).isEmpty()
    }

    @Test
    fun aTemplateThatCantBeLoadedIsTriedAgain() {
        load.state.value = Load.Unavailable(IllegalStateException("offline"))
        show(viewModel(uuid = "push"))

        compose.onNodeWithText("Couldn't load this template.").assertExists()
        load.state.value = Load.Ready(push())
        compose.onNodeWithText("Try again").performClick()

        compose.onNodeWithText("Push").assertExists()
        compose.onNodeWithTag(TemplateEditorTags.Save).assertExists()
    }

    private fun viewModel(uuid: String?) = TemplateEditorViewModel(
        dispatcher = Dispatchers.Main,
        uuid = uuid,
        defaultName = "My template",
        loadTemplate = load,
        saveTemplate = save,
        analytics = NoAnalytics,
    )

    private fun show(viewModel: TemplateEditorViewModel, onAddExercise: () -> Unit = {}) = compose.setContent {
        Theme {
            TemplateEditorScreen(
                viewModel = viewModel,
                onAddExercise = onAddExercise,
                onOpenExercise = { _, _ -> },
                onExit = { exit = it },
            )
        }
    }

    // Bench press, 10, 8 and 8 reps, with 1:30 of rest
    private fun push() = ELRoutine(
        uuid = "push",
        name = "Push",
        exerciseGroups = mutableListOf(
            ELExerciseGroup(
                uuid = "group",
                type = "SINGLE",
                restTimeSeconds = 90,
                exercises = mutableListOf(
                    ELRoutineExercise("item", ELExercise(uuid = "bench", name = "Bench press"), mutableListOf(ELSet(requiredReps = 10), ELSet(requiredReps = 8), ELSet(requiredReps = 8))),
                ),
            ),
        ),
    )

    // As the picker hands an exercise over: one empty set
    private fun picked(name: String) = ELExerciseGroup(
        uuid = "group-$name",
        type = "SINGLE",
        exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(ELExercise(uuid = name, name = name))),
    )

    private fun cell(block: Int, round: Int): SemanticsNodeInteraction {
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToIndex(block + 1)
        return compose.onNodeWithTag(ExerciseTableTags.count(ExerciseTableTags.set(block, round, 0)))
    }

    // A key or button on the docked keypad, rather than the same text in the table
    private fun key(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(TemplateEditorTags.Keypad)))

    private fun keys(digits: String) = digits.forEach { key(it.toString()).performClick() }

    private class FakeLoadTemplate : LoadTemplateUseCase {
        val state = MutableStateFlow<Load<ELRoutine>>(Load.Loading)

        override fun invoke(uuid: String): Flow<Load<ELRoutine>> = state
    }

    private class FakeSaveTemplate : SaveTemplateUseCase {
        val saved = mutableListOf<ELRoutine>()

        override suspend fun invoke(routine: ELRoutine): Either<Throwable, Unit> {
            saved += routine
            return Either.Right(Unit)
        }
    }

    private object NoAnalytics : TemplateEditorAnalytics {
        override fun screenViewed() = Unit
        override fun templateSaved(isNew: Boolean) = Unit
        override fun exercisesAdded() = Unit
        override fun setAdded() = Unit
        override fun setDeleted() = Unit
        override fun restChanged(seconds: Int) = Unit
        override fun timeForAllSets(seconds: Int) = Unit
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
