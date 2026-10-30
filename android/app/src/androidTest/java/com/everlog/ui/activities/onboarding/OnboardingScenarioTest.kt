package com.everlog.ui.activities.onboarding

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.core.Either
import arrow.core.left
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.data.controllers.starterroutines.BuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.SaveStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.StarterRoutineGenerator
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The onboarding driven as a user would, with the real view-model. The templates are built from the answers as the
 * app builds them, over an exercise library made to fit; saving is kept here. A few journeys rather than a test per
 * detail: the view-model's unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScenarioTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val settings = RecordingSettings()
    private val saved = mutableListOf<List<ELRoutine>>()
    private var buildFailures = 0
    private var closed = false
    private var builderOpened = false

    @Test
    fun answeringTheQuestionsAndKeepingTheTemplates() {
        show()

        compose.onNodeWithText("Let's go").performClick()
        compose.onNodeWithText("Pounds").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithContentDescription("4 days a week").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Gym").performClick()
        compose.onNodeWithText("Next").performClick()
        // Any question can be skipped
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Build muscle").performClick()
        compose.onNodeWithText("Next").performClick()

        // Units and days a week are settings, saved as they're answered
        assertThat(settings.unit).isEqualTo(WeightUnit.POUND)
        assertThat(settings.weeklyGoal).isEqualTo(4)

        compose.onNodeWithText("Build my templates").performClick()
        compose.waitUntil(WaitMillis) { exists("Looks good") }
        compose.onNodeWithText("Looks good").performClick()

        compose.waitUntil(WaitMillis) { closed }
        // Four days a week is an upper / lower split
        assertThat(saved.single().map { it.name }).containsExactly("Upper", "Lower")
    }

    @Test
    fun aBuildThatFailsThenTheUsersOwnTemplate() {
        buildFailures = 1
        val viewModel = show()

        compose.onNodeWithText("Let's go").performClick()
        repeat(5) { compose.onNodeWithText("Skip").performClick() }
        compose.onNodeWithText("Build my templates").performClick()

        // Building fails once, and Try again builds them
        compose.waitUntil(WaitMillis) { exists("Try again") }
        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(WaitMillis) { exists("Build my own template") }

        // The routine builder is another screen, whose saved routine is handed straight in
        compose.onNodeWithText("Build my own template").performClick()
        compose.waitUntil(WaitMillis) { builderOpened }
        viewModel.onOwnRoutineSaved()

        compose.waitUntil(WaitMillis) { closed }
        assertThat(saved).isEmpty()
    }

    @Test
    fun skippingSetup() {
        show()

        // Back on the welcome asks about skipping setup, and Keep going stays
        Espresso.pressBack()
        compose.onNode(hasText("Keep going") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Let's go").assertExists()

        // On a question, the top bar's Skip setup asks too, and the prompt's closes the screen
        compose.onNodeWithText("Let's go").performClick()
        compose.waitUntil(WaitMillis) { exists("Skip setup") }
        compose.onNodeWithText("Skip setup").performClick()
        compose.onNode(hasText("Skip setup") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(WaitMillis) { closed }
        assertThat(settings.unit).isNull()
    }

    private fun show(): OnboardingViewModel {
        val viewModel = OnboardingViewModel(
            dispatcher = Dispatchers.Main,
            buildStarterRoutinesUseCase = object : BuildStarterRoutinesUseCase {
                override suspend fun invoke(dto: BuildStarterRoutinesUseCase.Dto): Either<Throwable, BuildStarterRoutinesUseCase.Result> {
                    if (buildFailures-- > 0) return IOException("Offline").left()
                    val week = StarterRoutineGenerator.generate(dto.profile)
                    val library = week.routines.flatMap { it.exercises }.map { ELExercise(uuid = it.exerciseId, name = it.exerciseId) }
                    return week.toRoutines(library, dto.createdDate).map { BuildStarterRoutinesUseCase.Result(week, it) }
                }
            },
            saveStarterRoutinesUseCase = object : SaveStarterRoutinesUseCase {
                override suspend fun invoke(dto: SaveStarterRoutinesUseCase.Dto): Either<Throwable, Unit> {
                    saved += dto.routines
                    return Either.Right(Unit)
                }
            },
            settings = settings,
            analytics = NoAnalytics,
        )
        compose.setContent {
            Theme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onOpenRoutineBuilder = { builderOpened = true },
                    onClose = { closed = true },
                )
            }
        }
        return viewModel
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private class RecordingSettings : OnboardingSettings {
        var unit: WeightUnit? = null
        var weeklyGoal: Int? = null

        override fun setWeightUnit(unit: WeightUnit) {
            this.unit = unit
        }

        override fun setWeeklyWorkoutsGoal(count: Int) {
            weeklyGoal = count
        }
    }

    private object NoAnalytics : OnboardingAnalytics {
        override fun screenViewed(step: OnboardingStep) = Unit
        override fun stepViewed(step: String) = Unit
        override fun stepCompleted(step: String, value: String?) = Unit
        override fun stepSkipped(step: String) = Unit
        override fun stepFailed(step: String) = Unit
        override fun stepRetried(step: String) = Unit
        override fun questionReopened(step: String) = Unit
        override fun questionEdited(step: String, value: String?) = Unit
        override fun skipPromptShown(step: String) = Unit
        override fun skipPromptCancelled(step: String) = Unit
        override fun templateToggled(open: Boolean) = Unit
        override fun finished(outcome: String, step: String, routines: Int) = Unit
    }

    private companion object {
        // The building and saving animations play through in test time, one frame at a time, so they take ~35s
        // where frames are slow to draw (the API 37 tablet emulator)
        const val WaitMillis = 60_000L
    }
}
