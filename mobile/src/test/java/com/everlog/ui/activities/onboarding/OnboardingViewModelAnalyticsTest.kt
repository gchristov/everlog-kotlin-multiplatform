package com.everlog.ui.activities.onboarding

import arrow.core.Either
import com.everlog.data.controllers.starterroutines.RealBuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.RealSaveStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.StarterRoutineGenerator
import com.everlog.managers.preferences.SettingsManager
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.FakeExerciseRepository
import com.everlog.testutil.FakeRoutineRepository
import com.everlog.testutil.exerciseLibrary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class OnboardingViewModelAnalyticsTest {

    private val analytics = FakeOnboardingAnalytics()
    private val exerciseRepository = FakeExerciseRepository()
    private val routineRepository = FakeRoutineRepository()

    private fun viewModel() = OnboardingViewModel(
        dispatcher = FakeCoroutineDispatcher,
        buildStarterRoutinesUseCase = RealBuildStarterRoutinesUseCase(FakeCoroutineDispatcher, exerciseRepository),
        saveStarterRoutinesUseCase = RealSaveStarterRoutinesUseCase(FakeCoroutineDispatcher, routineRepository),
        settings = FakeOnboardingSettings(),
        analytics = analytics,
        now = { 0L },
    )

    // Every question answered with these, so the exercise library can hold the week they build
    private fun OnboardingViewModel.answerAll() {
        onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Kilograms)
        onSubmit()
        onDaysSelect(OnboardingQuestions.Days, 3)
        onSubmit()
        onOptionSelect(OnboardingQuestions.Where, OnboardingQuestions.Gym)
        onSubmit()
        onSkip()
        onOptionSelect(OnboardingQuestions.Goal, OnboardingQuestions.BuildMuscle)
        onSubmit()
    }

    private fun libraryForAnswers() {
        val answers = viewModel().run {
            onStart()
            answerAll()
            state.value.answers
        }
        exerciseRepository.globalExercises = Either.Right(exerciseLibrary(StarterRoutineGenerator.generate(OnboardingQuestions.starterProfile(answers))))
        analytics.events.clear()
    }

    @Test
    fun `logs each step viewed once and each answer`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.answerAll()

        assertThat(analytics.events).containsExactly(
            "viewed welcome",
            "completed welcome null",
            "viewed units",
            "completed units kg",
            "viewed days",
            "completed days 3",
            "viewed where",
            "completed where gym",
            "viewed experience",
            "skipped experience",
            "viewed goal",
            "completed goal muscle",
        ).inOrder()
    }

    @Test
    fun `editing an answer logs the edit, not the question again`() {
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Kilograms)
        viewModel.onSubmit()
        analytics.events.clear()

        viewModel.onEdit(OnboardingQuestions.Units)
        viewModel.onOptionSelect(OnboardingQuestions.Units, OnboardingQuestions.Pounds)
        viewModel.onSubmit()

        assertThat(analytics.events).containsExactly("reopened units", "edited units lb").inOrder()
    }

    @Test
    fun `skip setup logs the prompt and where it finished`() {
        val viewModel = viewModel()
        viewModel.onSkipSetup()
        viewModel.onKeepGoing()
        viewModel.onStart()
        viewModel.onSkipSetup()
        viewModel.onConfirmSkipSetup()

        assertThat(analytics.events).containsExactly(
            "viewed welcome",
            "prompt shown welcome",
            "prompt cancelled welcome",
            "completed welcome null",
            "viewed units",
            "prompt shown units",
            "finished skipped units 0",
        ).inOrder()
        assertThat(viewModel.state.value.finished).isTrue()
    }

    @Test
    fun `looks good logs building, the reveal, saving and the starter templates outcome`() {
        libraryForAnswers()
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.answerAll()
        analytics.events.clear()

        viewModel.onBuild()
        viewModel.onBuildShown()
        viewModel.onRoutineToggle(1)
        viewModel.onLooksGood()

        assertThat(analytics.events).containsExactly(
            "viewed building",
            "completed building null",
            "viewed reveal",
            "toggled true",
            "completed reveal looks_good",
            "viewed saving",
            "completed saving null",
            "finished starter_templates saving 3",
        ).inOrder()
    }

    @Test
    fun `build my own template logs the own template outcome`() {
        libraryForAnswers()
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.answerAll()
        viewModel.onBuild()
        viewModel.onBuildShown()
        analytics.events.clear()

        viewModel.onBuildOwn()
        viewModel.onRoutineBuilderOpened()
        viewModel.onOwnRoutineSaved()

        assertThat(analytics.events).containsExactly(
            "completed reveal build_own",
            "finished own_template reveal 1",
        ).inOrder()
    }

    @Test
    fun `a failed build logs the failure, the retry and the skip`() {
        exerciseRepository.globalExercises = Either.Left(IOException("Offline"))
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.answerAll()
        analytics.events.clear()

        viewModel.onBuild()
        viewModel.onRetryBuild()
        viewModel.onSkipBuild()

        assertThat(analytics.events).containsExactly(
            "viewed building",
            "failed building",
            "retried building",
            "failed building",
            "skipped building",
            "finished skipped building 0",
        ).inOrder()
    }

    @Test
    fun `a failed save logs the failure and the skip`() {
        libraryForAnswers()
        routineRepository.saveRoutines = Either.Left(IOException("Offline"))
        val viewModel = viewModel()
        viewModel.onStart()
        viewModel.answerAll()
        viewModel.onBuild()
        viewModel.onBuildShown()
        analytics.events.clear()

        viewModel.onLooksGood()
        viewModel.onRetrySave()
        viewModel.onSkipSave()

        assertThat(analytics.events).containsExactly(
            "completed reveal looks_good",
            "viewed saving",
            "failed saving",
            "retried saving",
            "failed saving",
            "skipped saving",
            "finished skipped saving 0",
        ).inOrder()
    }
}

private class FakeOnboardingAnalytics : OnboardingAnalytics {
    val events = mutableListOf<String>()

    override fun stepViewed(step: String) { events += "viewed $step" }
    override fun stepCompleted(step: String, value: String?) { events += "completed $step $value" }
    override fun stepSkipped(step: String) { events += "skipped $step" }
    override fun stepFailed(step: String) { events += "failed $step" }
    override fun stepRetried(step: String) { events += "retried $step" }
    override fun questionReopened(step: String) { events += "reopened $step" }
    override fun questionEdited(step: String, value: String?) { events += "edited $step $value" }
    override fun skipPromptShown(step: String) { events += "prompt shown $step" }
    override fun skipPromptCancelled(step: String) { events += "prompt cancelled $step" }
    override fun templateToggled(open: Boolean) { events += "toggled $open" }
    override fun finished(outcome: String, step: String, routines: Int) { events += "finished $outcome $step $routines" }
}

private class FakeOnboardingSettings : OnboardingSettings {
    override fun setWeightUnit(unit: SettingsManager.WeightUnit) = Unit
    override fun setWeeklyWorkoutsGoal(count: Int) = Unit
}
