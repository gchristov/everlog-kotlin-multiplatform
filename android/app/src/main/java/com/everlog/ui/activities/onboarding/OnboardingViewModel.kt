package com.everlog.ui.activities.onboarding

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.data.controllers.starterroutines.BuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.SaveStarterRoutinesUseCase
import com.everlog.data.model.ELRoutine
import com.everlog.managers.analytics.AnalyticsConstants
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.DayOfWeek
import timber.log.Timber

/**
 * The welcome, then the setup questionnaire from the Everlog Onboarding design: one question open
 * at a time, answered questions collapse to summary rows that can be reopened, then Build my
 * templates builds the starter routines behind the building screen, and the reveal shows them.
 * Looks good saves them behind the saving screen, then the screen closes. Build my own template
 * opens the routine builder instead: the screen closes once the user's own routine is saved, and
 * backing out of the builder returns to the reveal.
 *
 * Answers that are app settings (units, and days a week as the weekly workouts goal) are saved as
 * soon as they're given. If building or saving the routines fails, the user can try again or skip.
 *
 * Every step logs when it's first viewed and how it's left (completed, skipped, failed), and
 * [OnboardingAnalytics.finished] logs how the whole onboarding ended.
 */
class OnboardingViewModel(
    dispatcher: CoroutineDispatcher,
    private val buildStarterRoutinesUseCase: BuildStarterRoutinesUseCase,
    private val saveStarterRoutinesUseCase: SaveStarterRoutinesUseCase,
    private val settings: OnboardingSettings,
    private val analytics: OnboardingAnalytics,
    private val now: () -> Long = System::currentTimeMillis,
    questions: List<OnboardingQuestion> = OnboardingQuestions.all,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) : CommonViewModel<OnboardingState>(
    dispatcher = dispatcher,
    initialState = OnboardingState(questions = questions, activeQuestionId = null, firstDayOfWeek = firstDayOfWeek).activate(questions.firstOrNull()?.id)
) {
    // Steps already logged as viewed, so going back to one (e.g. after editing an answer) isn't
    // counted again
    private val viewedSteps = mutableSetOf<String>()

    init {
        logViewed(AnalyticsConstants.ONBOARDING_STEP_WELCOME)
    }

    fun onScreenShown() = analytics.screenViewed(state.value.step)

    // Let's go on the welcome
    fun onStart() {
        if (state.value.step != OnboardingStep.Welcome) return
        setState { copy(step = OnboardingStep.Questions) }
        analytics.stepCompleted(AnalyticsConstants.ONBOARDING_STEP_WELCOME)
        logNextQuestionViewed()
    }

    fun onOptionSelect(questionId: String, optionId: String) {
        updateInput(questionId) { Answer.Choice(optionId) }
    }

    fun onDaysSelect(questionId: String, count: Int) {
        updateInput(questionId) { Answer.Days(count) }
    }

    fun onReminderDayToggle(questionId: String, day: DayOfWeek) {
        updateInput(questionId) { input ->
            val reminders = input as? Answer.Reminders ?: return@updateInput input
            reminders.copy(days = if (day in reminders.days) reminders.days - day else reminders.days + day)
        }
    }

    fun onReminderTimeChange(questionId: String, hour: Int, minute: Int) {
        updateInput(questionId) { input ->
            (input as? Answer.Reminders)?.copy(hour = hour, minute = minute) ?: input
        }
    }

    // Next, Done (when editing) and Remind me
    fun onSubmit() {
        val currentState = state.value
        val questionId = currentState.activeQuestionId ?: return
        val answer = currentState.input ?: return
        if (!currentState.canContinue) return
        setState { answer(questionId, answer, skipped = false) }
        saveSetting(questionId, answer)
        if (currentState.isEditing) {
            analytics.questionEdited(questionId, analyticsValue(answer))
        } else {
            analytics.stepCompleted(questionId, analyticsValue(answer))
        }
        logNextQuestionViewed()
    }

    fun onSkip() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, OnboardingQuestions.skippedAnswer(questionId), skipped = true) }
        analytics.stepSkipped(questionId)
        logNextQuestionViewed()
    }

    fun onNotNow() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, Answer.RemindersOff, skipped = false) }
        analytics.stepCompleted(questionId, analyticsValue(Answer.RemindersOff))
        logNextQuestionViewed()
    }

    fun onEdit(questionId: String) {
        if (questionId !in state.value.answers) return
        setState { activate(questionId) }
        analytics.questionReopened(questionId)
    }

    // Leaves an edit without changing the answer
    fun onCancel() {
        setState { activate(firstUnansweredId) }
    }

    // Skip setup in the top bar, or Back, on the welcome or a question: asks first
    fun onSkipSetup() {
        val currentState = state.value
        if (currentState.skipSetupPrompt || currentState.finished || (currentState.step != OnboardingStep.Welcome && currentState.step != OnboardingStep.Questions)) return
        setState { copy(skipSetupPrompt = true) }
        analytics.skipPromptShown(currentState.analyticsStep)
    }

    // Keep going on the skip setup prompt
    fun onKeepGoing() {
        if (!state.value.skipSetupPrompt) return
        setState { copy(skipSetupPrompt = false) }
        analytics.skipPromptCancelled(state.value.analyticsStep)
    }

    // Skip setup on the prompt: the screen closes with nothing set up beyond the answered settings
    fun onConfirmSkipSetup() {
        if (!state.value.skipSetupPrompt) return
        setState { copy(skipSetupPrompt = false, finished = true) }
        analytics.finished(AnalyticsConstants.ONBOARDING_OUTCOME_SKIPPED, state.value.analyticsStep, routines = 0)
    }

    fun onBuild() {
        val currentState = state.value
        if (!currentState.allAnswered || currentState.step == OnboardingStep.Building) return
        setState { copy(step = OnboardingStep.Building) }
        logViewed(AnalyticsConstants.ONBOARDING_STEP_BUILDING)
        build(currentState.answers)
    }

    // Try again after the build failed: builds the week from scratch
    fun onRetryBuild() {
        val currentState = state.value
        if (currentState.step != OnboardingStep.Building || currentState.build != StarterRoutinesProgress.Failed) return
        setState { copy(build = StarterRoutinesProgress.InProgress, buildAttempt = buildAttempt + 1) }
        analytics.stepRetried(AnalyticsConstants.ONBOARDING_STEP_BUILDING)
        build(currentState.answers)
    }

    // Skip after the build failed, so the user is never stuck here
    fun onSkipBuild() {
        if (state.value.step != OnboardingStep.Building || state.value.build != StarterRoutinesProgress.Failed) return
        setState { copy(finished = true) }
        analytics.stepSkipped(AnalyticsConstants.ONBOARDING_STEP_BUILDING)
        analytics.finished(AnalyticsConstants.ONBOARDING_OUTCOME_SKIPPED, AnalyticsConstants.ONBOARDING_STEP_BUILDING, routines = 0)
    }

    // The building screen has played the whole success animation
    fun onBuildShown() {
        if (state.value.step != OnboardingStep.Building || state.value.build != StarterRoutinesProgress.Done) return
        setState { copy(step = OnboardingStep.Reveal) }
        logViewed(AnalyticsConstants.ONBOARDING_STEP_REVEAL)
    }

    // Opens or closes a routine's card on the reveal
    fun onRoutineToggle(index: Int) {
        val open = index !in state.value.openRoutines
        setState { copy(openRoutines = if (open) openRoutines + index else openRoutines - index) }
        analytics.templateToggled(open)
    }

    // Looks good on the reveal: saves the starter routines behind the saving screen
    fun onLooksGood() {
        val currentState = state.value
        val starter = currentState.starter ?: return
        if (currentState.step != OnboardingStep.Reveal) return
        setState { copy(step = OnboardingStep.Saving) }
        analytics.stepCompleted(AnalyticsConstants.ONBOARDING_STEP_REVEAL, AnalyticsConstants.ONBOARDING_REVEAL_LOOKS_GOOD)
        logViewed(AnalyticsConstants.ONBOARDING_STEP_SAVING)
        save(starter.routines)
    }

    // Build my own template on the reveal: opens the routine builder. The starter routines aren't
    // saved, so backing out of the builder returns to the reveal as it was.
    fun onBuildOwn() {
        if (state.value.step != OnboardingStep.Reveal) return
        setState { copy(openRoutineBuilder = true) }
        analytics.stepCompleted(AnalyticsConstants.ONBOARDING_STEP_REVEAL, AnalyticsConstants.ONBOARDING_REVEAL_BUILD_OWN)
    }

    // The screen has opened the routine builder
    fun onRoutineBuilderOpened() {
        setState { copy(openRoutineBuilder = false) }
    }

    // The routine builder saved the user's own routine. Closes whatever the step: after process
    // death the screen starts over, but the routine is saved, so the user is done here.
    fun onOwnRoutineSaved() {
        setState { copy(finished = true) }
        analytics.finished(AnalyticsConstants.ONBOARDING_OUTCOME_OWN_TEMPLATE, AnalyticsConstants.ONBOARDING_STEP_REVEAL, routines = 1)
    }

    // Try again after saving failed. The routines keep their uuids, so a save that got through
    // after all is overwritten rather than doubled.
    fun onRetrySave() {
        val currentState = state.value
        val starter = currentState.starter ?: return
        if (currentState.step != OnboardingStep.Saving || currentState.save != StarterRoutinesProgress.Failed) return
        setState { copy(save = StarterRoutinesProgress.InProgress, saveAttempt = saveAttempt + 1) }
        analytics.stepRetried(AnalyticsConstants.ONBOARDING_STEP_SAVING)
        save(starter.routines)
    }

    // Skip after saving failed: the screen closes without the routines
    fun onSkipSave() {
        if (state.value.step != OnboardingStep.Saving || state.value.save != StarterRoutinesProgress.Failed) return
        setState { copy(finished = true) }
        analytics.stepSkipped(AnalyticsConstants.ONBOARDING_STEP_SAVING)
        analytics.finished(AnalyticsConstants.ONBOARDING_OUTCOME_SKIPPED, AnalyticsConstants.ONBOARDING_STEP_SAVING, routines = 0)
    }

    // The saving screen has played the whole success animation
    fun onSaveShown() {
        if (state.value.step != OnboardingStep.Saving || state.value.save != StarterRoutinesProgress.Done) return
        setState { copy(finished = true) }
    }

    private fun build(answers: Map<String, Answer>) {
        launchCoroutine {
            val profile = OnboardingQuestions.starterProfile(answers)
            buildStarterRoutinesUseCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = now())).fold(
                // Reported as a non-fatal. The screen offers to try again or skip.
                ifLeft = {
                    Timber.tag(TAG).e(it)
                    setState { copy(build = StarterRoutinesProgress.Failed) }
                    analytics.stepFailed(AnalyticsConstants.ONBOARDING_STEP_BUILDING)
                },
                ifRight = { starter ->
                    Timber.tag(TAG).i("Built starter routines for %s: %s", profile, starter.routines.joinToString { "${it.name} (${it.getTotalExercises()} exercises)" })
                    setState { copy(build = StarterRoutinesProgress.Done, starter = starter) }
                    analytics.stepCompleted(AnalyticsConstants.ONBOARDING_STEP_BUILDING)
                },
            )
        }
    }

    private fun save(routines: List<ELRoutine>) {
        launchCoroutine {
            saveStarterRoutinesUseCase(SaveStarterRoutinesUseCase.Dto(routines)).fold(
                // Reported as a non-fatal. The screen offers to try again or skip.
                ifLeft = {
                    Timber.tag(TAG).e(it)
                    setState { copy(save = StarterRoutinesProgress.Failed) }
                    analytics.stepFailed(AnalyticsConstants.ONBOARDING_STEP_SAVING)
                },
                ifRight = {
                    Timber.tag(TAG).i("Saved starter routines: %s", routines.joinToString { it.name.orEmpty() })
                    setState { copy(save = StarterRoutinesProgress.Done) }
                    analytics.stepCompleted(AnalyticsConstants.ONBOARDING_STEP_SAVING)
                    analytics.finished(AnalyticsConstants.ONBOARDING_OUTCOME_STARTER_TEMPLATES, AnalyticsConstants.ONBOARDING_STEP_SAVING, routines.size)
                },
            )
        }
    }

    // Skipped questions leave the settings as they are
    private fun saveSetting(questionId: String, answer: Answer) {
        when (questionId) {
            OnboardingQuestions.Units -> settings.setWeightUnit(
                if ((answer as Answer.Choice).optionId == OnboardingQuestions.Pounds) WeightUnit.POUND else WeightUnit.KILOGRAM
            )
            OnboardingQuestions.Days -> settings.setWeeklyWorkoutsGoal((answer as Answer.Days).count)
        }
    }

    // The question that opens after an answer, unless it was an edit and every question is answered
    private fun logNextQuestionViewed() {
        val currentState = state.value
        if (currentState.step != OnboardingStep.Questions || currentState.isEditing) return
        currentState.activeQuestionId?.let { logViewed(it) }
    }

    private fun logViewed(step: String) {
        if (viewedSteps.add(step)) analytics.stepViewed(step)
    }

    // An answer as a short analytics value, e.g. kg, 3 or gym
    private fun analyticsValue(answer: Answer): String = when (answer) {
        is Answer.Choice -> answer.optionId
        is Answer.Days -> answer.count.toString()
        is Answer.Reminders -> "on"
        Answer.RemindersOff -> "off"
    }

    private fun updateInput(questionId: String, update: (Answer?) -> Answer?) {
        if (questionId != state.value.activeQuestionId) return
        setState { copy(input = update(input)) }
    }

    private companion object {
        const val TAG = "OnboardingViewModel"
    }
}
