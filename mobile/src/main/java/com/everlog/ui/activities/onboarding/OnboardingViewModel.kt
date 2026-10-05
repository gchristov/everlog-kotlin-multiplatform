package com.everlog.ui.activities.onboarding

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.DayOfWeek

/**
 * The setup questionnaire from the Everlog Onboarding design: one question open at a time, answered
 * questions collapse to summary rows that can be reopened, then Build my routine finishes.
 *
 * UI only for now: answers aren't saved, nothing is logged, and no routine is built.
 */
class OnboardingViewModel(
    dispatcher: CoroutineDispatcher,
    questions: List<OnboardingQuestion> = OnboardingQuestions.all,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) : CommonViewModel<OnboardingViewModel.State>(
    dispatcher = dispatcher,
    initialState = State(questions = questions, activeQuestionId = null, firstDayOfWeek = firstDayOfWeek).activate(questions.firstOrNull()?.id)
) {
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
    }

    fun onSkip() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, OnboardingQuestions.skippedAnswer(questionId), skipped = true) }
    }

    fun onNotNow() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, Answer.RemindersOff, skipped = false) }
    }

    fun onEdit(questionId: String) {
        if (questionId !in state.value.answers) return
        setState { activate(questionId) }
    }

    // Leaves an edit without changing the answer
    fun onCancel() {
        setState { activate(firstUnansweredId) }
    }

    fun onBuild() {
        if (!state.value.allAnswered) return
        setState { copy(finished = true) }
    }

    private fun updateInput(questionId: String, update: (Answer?) -> Answer?) {
        if (questionId != state.value.activeQuestionId) return
        setState { copy(input = update(input)) }
    }

    data class State(
        val questions: List<OnboardingQuestion>,
        val answers: Map<String, Answer> = emptyMap(),
        // Questions skipped, whose answers are the defaults
        val skipped: Set<String> = emptySet(),
        // The open question: the next one to answer, or an answered one being edited
        val activeQuestionId: String?,
        // In-progress answer for the open question
        val input: Answer? = null,
        // Build my routine was tapped, so the screen closes
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

        // The step shown as "{n} of 6": the next question to answer, even while editing
        val step: Int
            get() = questions.indexOfFirst { it.id !in answers }.let { if (it == -1) questions.size else it + 1 }

        val canContinue: Boolean
            get() = when (val input = input) {
                is Answer.Reminders -> input.days.isNotEmpty()
                null -> false
                else -> true
            }

        internal fun answer(questionId: String, answer: Answer, skipped: Boolean): State {
            val newState = copy(
                answers = answers + (questionId to answer),
                skipped = if (skipped) this.skipped + questionId else this.skipped - questionId,
            )
            return newState.activate(newState.firstUnansweredId)
        }

        // Opens a question, starting from its answer, or what the design preselects
        internal fun activate(questionId: String?): State {
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
}
