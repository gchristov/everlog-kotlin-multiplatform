package com.everlog.android.onboarding

import com.everlog.android.onboarding.data.BuildStarterRoutinesUseCase
import org.threeten.bp.DayOfWeek

// What the screen shows, in order
enum class OnboardingStep {
    // Until Let's go
    Welcome,
    // One question open at a time, until Build my templates
    Questions,
    // The starter routines building, until they're ready or the user skips
    Building,
    // The starter routines, until Looks good or Build my own template
    Reveal,
    // The starter routines saving after Looks good, until they're saved or the user skips
    Saving,
}

// Building or saving the starter routines
enum class StarterRoutinesProgress {
    InProgress,
    Done,
    // Until Try again or Skip
    Failed,
}

data class OnboardingState(
    val questions: List<OnboardingQuestion>,
    val step: OnboardingStep = OnboardingStep.Welcome,
    val answers: Map<String, Answer> = emptyMap(),
    // Questions skipped, whose answers are the defaults
    val skipped: Set<String> = emptySet(),
    // The open question: the next one to answer, or an answered one being edited
    val activeQuestionId: String?,
    // In-progress answer for the open question
    val input: Answer? = null,
    // While the step is Building
    val build: StarterRoutinesProgress = StarterRoutinesProgress.InProgress,
    // Counts Try again, so the building screen starts its animation over
    val buildAttempt: Int = 0,
    // Once the build is ready
    val starter: BuildStarterRoutinesUseCase.Result? = null,
    // While the step is Saving
    val save: StarterRoutinesProgress = StarterRoutinesProgress.InProgress,
    // Counts Try again, so the saving screen starts its animation over
    val saveAttempt: Int = 0,
    // The reveal's open routine cards, by index. The first starts open.
    val openRoutines: Set<Int> = setOf(0),
    // Until the screen opens the routine builder
    val openRoutineBuilder: Boolean = false,
    // Skip setup asks first, on the welcome and the questions
    val skipSetupPrompt: Boolean = false,
    // The screen closes
    val finished: Boolean = false,
    // From Settings, for the order of the reminder days
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) {
    val firstUnansweredId: String? get() = questions.firstOrNull { it.id !in answers }?.id

    val activeQuestion: OnboardingQuestion? get() = questions.firstOrNull { it.id == activeQuestionId }

    val isEditing: Boolean get() = activeQuestionId != null && activeQuestionId in answers

    // Every question up to the next one to answer. Later ones stay hidden.
    val visibleQuestions: List<OnboardingQuestion>
        get() {
            val firstUnanswered = questions.indexOfFirst { it.id !in answers }
            return if (firstUnanswered == -1) questions else questions.take(firstUnanswered + 1)
        }

    val allAnswered: Boolean get() = answers.size == questions.size

    // Where Skip setup was tapped: the welcome, or the open question
    val analyticsStep: String
        get() = if (step == OnboardingStep.Welcome) OnboardingAnalyticsValues.STEP_WELCOME else activeQuestionId ?: OnboardingAnalyticsValues.STEP_WELCOME

    val canContinue: Boolean
        get() = when (val input = input) {
            is Answer.Reminders -> input.days.isNotEmpty()
            null -> false
            else -> true
        }

    internal fun answer(questionId: String, answer: Answer, skipped: Boolean): OnboardingState {
        val newState = copy(
            answers = answers + (questionId to answer),
            skipped = if (skipped) this.skipped + questionId else this.skipped - questionId,
        )
        return newState.activate(newState.firstUnansweredId)
    }

    // Opens a question, starting from its answer, or what the design preselects
    internal fun activate(questionId: String?): OnboardingState {
        val question = questions.firstOrNull { it.id == questionId }
        // A skipped question or reminders turned off start again from the preselection
        val answer = answers[questionId]?.takeIf { questionId !in skipped && it != Answer.RemindersOff }
        return copy(
            activeQuestionId = questionId,
            input = answer ?: question?.let { initialInput(it) },
        )
    }

    private fun initialInput(question: OnboardingQuestion): Answer? = when (question.id) {
        // Preselected from the device's locale
        OnboardingQuestions.Units -> Answer.Choice(OnboardingQuestions.deviceUnit())
        // Days follow the days-a-week answer, at 18:00
        OnboardingQuestions.Reminders -> {
            val days = (answers[OnboardingQuestions.Days] as? Answer.Days)?.count ?: 3
            Answer.Reminders(OnboardingQuestions.trainingDays(days).toSet(), hour = 18, minute = 0)
        }
        else -> null
    }
}
