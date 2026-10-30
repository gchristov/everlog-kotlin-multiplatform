package com.everlog.ui.activities.onboarding

import arrow.core.Either
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.data.controllers.starterroutines.RealBuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.RealSaveStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.StarterRoutineGenerator
import com.everlog.testutil.FakeExerciseRepository
import com.everlog.testutil.FakeRoutineRepository
import com.everlog.testutil.exerciseLibrary
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Test

/**
 * How the onboarding moves through its steps. OnboardingViewModelAnalyticsTest covers what it logs.
 */
class OnboardingViewModelTest {

    private val analytics = FakeOnboardingAnalytics()
    private val settings = FakeOnboardingSettings()
    private val exerciseRepository = FakeExerciseRepository()
    private val routineRepository = FakeRoutineRepository()

    private fun viewModel() = OnboardingViewModel(
        dispatcher = FakeCoroutineDispatcher,
        buildStarterRoutinesUseCase = RealBuildStarterRoutinesUseCase(FakeCoroutineDispatcher, exerciseRepository),
        saveStarterRoutinesUseCase = RealSaveStarterRoutinesUseCase(FakeCoroutineDispatcher, routineRepository),
        settings = settings,
        analytics = analytics,
        now = { 0L },
    )

    private val OnboardingViewModel.current get() = state.value

    private fun OnboardingViewModel.answerAll() {
        onStart()
        onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Pounds)
        onSubmit()
        onDaysSelect(OnboardingQuestions.Days, 4)
        onSubmit()
        onOptionSelect(OnboardingQuestions.Where, OnboardingQuestions.Gym)
        onSubmit()
        onOptionSelect(OnboardingQuestions.Experience, OnboardingQuestions.AWhile)
        onSubmit()
        onOptionSelect(OnboardingQuestions.Goal, OnboardingQuestions.BuildMuscle)
        onSubmit()
    }

    // A library that holds the week the answers build
    private fun libraryFor(viewModel: OnboardingViewModel) {
        val profile = OnboardingQuestions.starterProfile(viewModel.current.answers)
        exerciseRepository.globalExercises = Either.Right(exerciseLibrary(StarterRoutineGenerator.generate(profile)))
    }

    private fun atReveal(): OnboardingViewModel {
        val viewModel = viewModel()
        viewModel.answerAll()
        libraryFor(viewModel)
        viewModel.onBuild()
        viewModel.onBuildShown()
        return viewModel
    }

    // The questions

    @Test
    fun `it starts on the welcome, then opens the first question with the device's unit preselected`() {
        val viewModel = viewModel()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Welcome)

        viewModel.onStart()

        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Questions)
        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Units)
        assertThat(viewModel.current.input).isEqualTo(Answer.Choice(OnboardingQuestions.deviceUnit()))
        assertThat(viewModel.current.visibleQuestions.map { it.id }).containsExactly(OnboardingQuestions.Units)
    }

    @Test
    fun `units and days a week are saved to Settings as soon as they're answered`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Pounds)
        viewModel.onSubmit()
        assertThat(settings.savedUnit).isEqualTo(WeightUnit.POUND)

        viewModel.onDaysSelect(OnboardingQuestions.Days, 4)
        viewModel.onSubmit()

        assertThat(settings.savedWeeklyGoal).isEqualTo(4)
        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Where)
    }

    @Test
    fun `a question can't be answered without a choice, and a skipped one leaves Settings alone`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onSkip()
        assertThat(settings.savedUnit).isNull()

        viewModel.onSubmit()

        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Days)
        assertThat(OnboardingQuestions.Days).isNotIn(viewModel.current.answers.keys)
    }

    @Test
    fun `an answer can be reopened and changed, or left as it was`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Kilograms)
        viewModel.onSubmit()

        viewModel.onEdit(OnboardingQuestions.Units)
        assertThat(viewModel.current.isEditing).isTrue()
        assertThat(viewModel.current.input).isEqualTo(Answer.Choice(OnboardingQuestions.Kilograms))
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Pounds)
        viewModel.onCancel()
        assertThat(viewModel.current.answers[OnboardingQuestions.Units]).isEqualTo(Answer.Choice(OnboardingQuestions.Kilograms))
        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Days)

        viewModel.onEdit(OnboardingQuestions.Units)
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Pounds)
        viewModel.onSubmit()
        assertThat(viewModel.current.answers[OnboardingQuestions.Units]).isEqualTo(Answer.Choice(OnboardingQuestions.Pounds))
        assertThat(settings.savedUnit).isEqualTo(WeightUnit.POUND)
    }

    @Test
    fun `a question not reached yet can't be opened, and choices only go to the open question`() {
        val viewModel = viewModel()
        viewModel.onStart()

        viewModel.onEdit(OnboardingQuestions.Goal)
        viewModel.onOptionSelect(OnboardingQuestions.Goal, OnboardingQuestions.BuildMuscle)

        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Units)
        assertThat(viewModel.current.input).isNotEqualTo(Answer.Choice(OnboardingQuestions.BuildMuscle))
    }

    // Skipping setup

    @Test
    fun `skip setup asks first, and keep going carries on where it was`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onSkipSetup()
        assertThat(viewModel.current.skipSetupPrompt).isTrue()

        viewModel.onKeepGoing()

        assertThat(viewModel.current.skipSetupPrompt).isFalse()
        assertThat(viewModel.current.finished).isFalse()
        assertThat(viewModel.current.activeQuestionId).isEqualTo(OnboardingQuestions.Units)
    }

    @Test
    fun `confirming skip setup closes the screen`() {
        val viewModel = viewModel()
        viewModel.onSkipSetup()
        viewModel.onConfirmSkipSetup()

        assertThat(viewModel.current.finished).isTrue()
    }

    @Test
    fun `setup can't be skipped once the templates are built`() {
        val viewModel = atReveal()

        viewModel.onSkipSetup()

        assertThat(viewModel.current.skipSetupPrompt).isFalse()
    }

    // Building

    @Test
    fun `templates build only once every question is answered, then the reveal shows them`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onBuild()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Questions)

        viewModel.answerAll()
        libraryFor(viewModel)
        viewModel.onBuild()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Building)
        assertThat(viewModel.current.build).isEqualTo(StarterRoutinesProgress.Done)

        viewModel.onBuildShown()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Reveal)
        assertThat(viewModel.current.starter?.routines).isNotEmpty()
        assertThat(viewModel.current.openRoutines).containsExactly(0)
    }

    @Test
    fun `a failed build can be tried again, which starts its animation over, or skipped`() {
        exerciseRepository.globalExercises = Either.Left(IOException("Offline"))
        val viewModel = viewModel()
        viewModel.answerAll()
        viewModel.onBuild()
        assertThat(viewModel.current.build).isEqualTo(StarterRoutinesProgress.Failed)
        viewModel.onBuildShown()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Building)

        libraryFor(viewModel)
        viewModel.onRetryBuild()
        assertThat(viewModel.current.buildAttempt).isEqualTo(1)
        assertThat(viewModel.current.build).isEqualTo(StarterRoutinesProgress.Done)

        val failing = viewModel()
        exerciseRepository.globalExercises = Either.Left(IOException("Offline"))
        failing.answerAll()
        failing.onBuild()
        failing.onSkipBuild()
        assertThat(failing.current.finished).isTrue()
    }

    // The reveal

    @Test
    fun `the reveal's cards open and close`() {
        val viewModel = atReveal()

        viewModel.onRoutineToggle(0)
        viewModel.onRoutineToggle(1)

        assertThat(viewModel.current.openRoutines).containsExactly(1)
    }

    @Test
    fun `looks good saves the templates, and the screen closes once the saving animation has played`() {
        val viewModel = atReveal()
        val routines = viewModel.current.starter!!.routines

        viewModel.onLooksGood()
        assertThat(viewModel.current.step).isEqualTo(OnboardingStep.Saving)
        assertThat(routineRepository.saved).containsExactly(routines)
        assertThat(viewModel.current.finished).isFalse()

        viewModel.onSaveShown()
        assertThat(viewModel.current.finished).isTrue()
    }

    @Test
    fun `a failed save tries again with the same templates, or is skipped without them`() {
        val viewModel = atReveal()
        routineRepository.saveRoutines = Either.Left(IOException("Offline"))
        viewModel.onLooksGood()
        assertThat(viewModel.current.save).isEqualTo(StarterRoutinesProgress.Failed)

        viewModel.onRetrySave()
        assertThat(viewModel.current.saveAttempt).isEqualTo(1)
        assertThat(routineRepository.saved[1]).isEqualTo(routineRepository.saved[0])

        viewModel.onSkipSave()
        assertThat(viewModel.current.finished).isTrue()
    }

    @Test
    fun `build my own template opens the builder once, and its saved routine closes the screen`() {
        val viewModel = atReveal()

        viewModel.onBuildOwn()
        assertThat(viewModel.current.openRoutineBuilder).isTrue()
        viewModel.onRoutineBuilderOpened()
        assertThat(viewModel.current.openRoutineBuilder).isFalse()
        assertThat(routineRepository.saved).isEmpty()

        viewModel.onOwnRoutineSaved()
        assertThat(viewModel.current.finished).isTrue()
    }

    @Test
    fun `the screen view follows the step`() {
        val viewModel = viewModel()
        viewModel.onScreenShown()
        viewModel.onStart()
        viewModel.onScreenShown()

        assertThat(analytics.screens).containsExactly(OnboardingStep.Welcome, OnboardingStep.Questions).inOrder()
    }
}
