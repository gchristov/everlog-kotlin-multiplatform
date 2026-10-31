package com.everlog.android.exercise.picker

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.LoadExerciseLibraryUseCase
import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import com.everlog.android.exercise.data.model.library.ExerciseRegistry
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The exercise picker driven as a user would, with the real view-model over a small library. A few journeys rather
 * than a test per detail: the view-model's and filters' unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePickerScenarioTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private var result: PickerResult? = null
    private var opened: String? = null
    private var setTypeFor: Int? = null
    private var created: String? = null
    private var feedbackSent = 0
    private var missingReported = 0

    @Test
    fun pickingExercisesForAWorkout() {
        val viewModel = show(PickerMode.Pick)

        // With no feedback form, the menu only creates
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Create an exercise").assertExists()
        compose.onNodeWithText("Send feedback").assertDoesNotExist()
        Espresso.pressBack()

        // Typing searches names and aliases, and the keyboard's search key closes the menu over the list
        searchBar().performTextInput("bent over")
        searchBar().performImeAction()
        compose.waitUntil(WaitMillis) { !exists("Barbell Bench Press") }
        compose.onNodeWithText("Barbell Row").assertExists()
        searchBar().performTextClearance()
        searchBar().performImeAction()

        // A filter from its chip: Equipment is Dumbbell
        compose.onNodeWithText("Equipment").performClick()
        // The menu's value, with how many exercises it has
        compose.onNode(hasText("Dumbbell") and hasText("1")).performClick()
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(WaitMillis) { !exists("Barbell Row") }
        compose.onNodeWithText("Dumbbell Bench Press").assertExists()

        // Picked one by one, then added on its own
        compose.onNodeWithText("Dumbbell Bench Press").performClick()
        compose.onNodeWithText("Add 1 exercise").performClick()
        compose.waitUntil(WaitMillis) { result != null }
        assertThat(result).isEqualTo(PickerResult(listOf(DumbbellBench), ELSetType.SINGLE))

        // Two picks can go in as a superset, whose kind is picked on another screen
        result = null
        compose.onNodeWithText("Clear all").performClick()
        compose.waitUntil(WaitMillis) { exists("Barbell Row") }
        compose.onNodeWithText("Barbell Row").performClick()
        compose.onNodeWithText("Add as a superset").performClick()
        assertThat(setTypeFor).isEqualTo(2)
        viewModel.onAdd(ELSetType.SUPER)
        compose.waitUntil(WaitMillis) { result != null }
        assertThat(result).isEqualTo(PickerResult(listOf(DumbbellBench, BarbellRow), ELSetType.SUPER))
    }

    @Test
    fun browsingAndSearchingForWhatIsMissing() {
        show(PickerMode.Browse, feedbackForm = true)

        // A tap opens the exercise, and nothing is picked
        compose.onNodeWithText("Barbell Row").performClick()
        assertThat(opened).isEqualTo("Barbell Row")
        compose.onNodeWithText("Add 1 exercise").assertDoesNotExist()

        // Nothing found offers to create it, named as searched
        searchBar().performTextInput("zercher")
        searchBar().performImeAction()
        compose.waitUntil(WaitMillis) { exists("Create “zercher”", substring = true) }
        compose.onNodeWithText("Create “zercher”", substring = true).performClick()
        assertThat(created).isEqualTo("zercher")

        // Or tells us it's missing from the library
        compose.onNodeWithText("Tell us which exercise is missing").performClick()
        assertThat(missingReported).isEqualTo(1)

        // The menu sends any other feedback
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Send feedback").performClick()
        assertThat(feedbackSent).isEqualTo(1)
    }

    @Test
    fun aLibraryThatDoesNotLoad() {
        show(PickerMode.Pick, failures = 1)

        compose.waitUntil(WaitMillis) { exists("Couldn't load the exercises.") }
        compose.onNodeWithText("Try again").performClick()

        compose.waitUntil(WaitMillis) { exists("Barbell Row") }
    }

    private fun show(mode: PickerMode, failures: Int = 0, feedbackForm: Boolean = false): ExercisePickerViewModel {
        var failing = failures
        val viewModel = ExercisePickerViewModel(
            dispatcher = Dispatchers.Main,
            mode = mode,
            savedState = SavedStateHandle(),
            loadLibrary = object : LoadExerciseLibraryUseCase {
                override fun invoke(): Flow<Load<ExerciseLibrary>> =
                    flowOf(if (failing-- > 0) Load.Unavailable(IllegalStateException("offline")) else Load.Ready(Library))
            },
            recentSearches = object : ExercisePickerRecentSearches {
                override fun load(): List<List<FilterToken>> = emptyList()
                override fun save(searches: List<List<FilterToken>>) = Unit
            },
            analytics = object : ExercisePickerAnalytics {
                override fun screenViewed(mode: PickerMode) = Unit
                override fun filterAdded(filter: String, value: String, source: String) = Unit
                override fun filterRemoved(filter: String) = Unit
                override fun filterOperatorChanged(filter: String, not: Boolean) = Unit
                override fun searched(words: Int, results: Int) = Unit
                override fun noResults(text: String?, filters: Int) = Unit
                override fun exercisePicked(section: String, position: Int) = Unit
                override fun createTapped(source: String) = Unit
                override fun added(count: Int, setType: String, filters: Int, hadText: Boolean) = Unit
            },
            me = FilterValue(id = CreatedByMe, name = "Me"),
            maxPicks = 10,
        )
        compose.setContent {
            Theme {
                ExercisePickerScreen(
                    viewModel = viewModel,
                    onResult = { result = it },
                    onOpenExercise = { opened = it.name },
                    onCreateExercise = { created = it },
                    onPickSetType = { setTypeFor = it },
                    onSendFeedback = if (feedbackForm) ({ feedbackSent++ }) else null,
                    onReportMissing = if (feedbackForm) ({ missingReported++ }) else null,
                )
            }
        }
        if (failures == 0) compose.waitUntil(WaitMillis) { exists("Barbell Row") }
        return viewModel
    }

    private fun searchBar() = compose.onNode(hasSetTextAction())

    private fun exists(text: String, substring: Boolean = false) =
        compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L

        val BarbellBench = LibraryExercise(
            uuid = "bb_bench", name = "Barbell Bench Press", family = "bench_press", familyDefault = true,
            equipment = "barbell", primaryMuscles = listOf("pecs"),
        )
        val DumbbellBench = LibraryExercise(
            uuid = "db_bench", name = "Dumbbell Bench Press", family = "bench_press",
            equipment = "dumbbell", primaryMuscles = listOf("pecs"),
        )
        val BarbellRow = LibraryExercise(
            uuid = "bb_row", name = "Barbell Row", family = "row", familyDefault = true,
            equipment = "barbell", primaryMuscles = listOf("lats"), aliases = listOf("Bent-Over Row"),
        )

        val Library = ExerciseLibrary(
            exercises = listOf(BarbellBench, DumbbellBench, BarbellRow),
            registry = ExerciseRegistry(
                muscles = listOf(
                    ExerciseRegistry.Muscle(ExerciseRegistry.Entry("pecs", "Pecs"), group = "chest", side = "front"),
                    ExerciseRegistry.Muscle(ExerciseRegistry.Entry("lats", "Lats"), group = "back", side = "back"),
                ),
                muscleGroups = listOf(
                    ExerciseRegistry.MuscleGroup(ExerciseRegistry.Entry("chest", "Chest"), region = "upper", category = "CHEST"),
                    ExerciseRegistry.MuscleGroup(ExerciseRegistry.Entry("back", "Back"), region = "upper", category = "BACK"),
                ),
                equipment = listOf(
                    ExerciseRegistry.Equipment(ExerciseRegistry.Entry("barbell", "Barbell"), main = true),
                    ExerciseRegistry.Equipment(ExerciseRegistry.Entry("dumbbell", "Dumbbell"), main = true),
                ),
                families = listOf(
                    ExerciseRegistry.Family(ExerciseRegistry.Entry("row", "Row"), primaryMuscles = listOf("lats"), combination = false),
                    ExerciseRegistry.Family(ExerciseRegistry.Entry("bench_press", "Bench Press"), primaryMuscles = listOf("pecs"), combination = false),
                ),
            ),
            templateCounts = emptyMap(),
            lastDone = emptyMap(),
        )
    }
}
