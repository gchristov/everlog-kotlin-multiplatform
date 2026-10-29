package com.everlog.ui.fragments.home.exercise.info

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.Load
import com.everlog.data.controllers.exerciseguide.LoadExerciseGuideUseCase
import com.everlog.data.model.exercise.library.ExerciseGuide
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The exercise detail's Info tab with its real view-model: the guide once it loads, and what shows without one.
 * The tab sits in the XML exercise detail screen, so there's no activity of its own to run end to end.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseInfoScenarioTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(1, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    @Test
    fun aLibraryExerciseShowsItsGuideOnceItLoads() {
        val guide = MutableStateFlow<Load<ExerciseGuide>>(Load.Loading)
        show(Bench, loadGuide = guide)
        compose.onNodeWithText("B").assertExists()
        compose.onNodeWithText("How to do it").assertDoesNotExist()

        guide.value = Load.Ready(ExerciseGuide(instructions = listOf("Lie on the bench", "Press the bar up"), tips = listOf("Keep your feet down")))

        compose.waitUntil(WaitMillis) { exists("Press the bar up") }
        compose.onNodeWithText("How to do it").assertExists()
        compose.onNodeWithText("Lie on the bench").assertExists()
        compose.onNodeWithText("Tips").assertExists()
        compose.onNodeWithText("Keep your feet down").assertExists()
    }

    @Test
    fun aGuideThatDoesNotLoadLeavesTheExercise() {
        val viewModel = show(Bench, loadGuide = flowOf(Load.Unavailable(IOException("Offline"))))

        compose.onNodeWithText("B").assertExists()
        compose.onNodeWithText("How to do it").assertDoesNotExist()
        compose.onNodeWithText("Tips").assertDoesNotExist()

        // Renamed while the screen is open
        viewModel.updateExercise(Bench.copy(name = "Pause bench"))
        compose.waitUntil(WaitMillis) { exists("P") }
    }

    private fun show(exercise: ExerciseInfoViewModel.Exercise, loadGuide: Flow<Load<ExerciseGuide>>): ExerciseInfoViewModel {
        val viewModel = ExerciseInfoViewModel(
            dispatcher = Dispatchers.Main,
            loadGuide = object : LoadExerciseGuideUseCase {
                override fun invoke(uuid: String) = loadGuide
            },
            exercise = exercise,
        )
        compose.setContent { Theme { ExerciseInfoScreen(viewModel = viewModel) } }
        return viewModel
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L
        val Bench = ExerciseInfoViewModel.Exercise(uuid = "bb_bench", name = "Barbell Bench Press", imageUrl = null, own = false)
    }
}
