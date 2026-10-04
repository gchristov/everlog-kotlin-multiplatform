package com.everlog.ui.activities.questionform

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Asks [Question]s one at a time. The active question's input is submitted with [onNext], which
 * moves on to the first unanswered question. Answered questions can be reopened and changed without
 * losing the answers after them.
 */
class QuestionFormViewModel(
    dispatcher: CoroutineDispatcher,
    questions: List<Question> = SampleQuestions,
) : CommonViewModel<QuestionFormViewModel.State>(
    dispatcher = dispatcher,
    initialState = State(questions = questions, activeQuestionId = null).activate(questions.firstOrNull()?.id)
) {
    fun onYesNoSelect(questionId: String, value: Boolean) {
        updateInput(questionId) { Answer.YesNo(value) }
    }

    fun onOptionSelect(questionId: String, optionId: String) {
        updateInput(questionId) { Answer.SingleChoice(optionId) }
    }

    fun onOptionToggle(questionId: String, optionId: String) {
        updateInput(questionId) { input ->
            val selected = (input as? Answer.MultiChoice)?.optionIds ?: emptySet()
            Answer.MultiChoice(if (optionId in selected) selected - optionId else selected + optionId)
        }
    }

    fun onNumberChange(questionId: String, value: Int) {
        val question = state.value.activeQuestion as? Question.Number ?: return
        updateInput(questionId) { Answer.Number(value.coerceIn(question.min, question.max)) }
    }

    fun onNext() {
        val currentState = state.value
        val questionId = currentState.activeQuestionId ?: return
        val answer = currentState.input ?: return
        if (!currentState.canContinue) return
        setState {
            val newAnswers = answers + (questionId to answer)
            copy(answers = newAnswers).activate(questions.firstOrNull { it.id !in newAnswers }?.id)
        }
    }

    fun onEdit(questionId: String) {
        setState { activate(questionId) }
    }

    fun onRestart() {
        setState { State(questions = questions, activeQuestionId = null).activate(questions.firstOrNull()?.id) }
    }

    private fun updateInput(questionId: String, update: (Answer?) -> Answer) {
        if (questionId != state.value.activeQuestionId) return
        setState { copy(input = update(input)) }
    }

    data class State(
        val questions: List<Question>,
        val answers: Map<String, Answer> = emptyMap(),
        // Null once every question is answered and none is being edited
        val activeQuestionId: String?,
        // In-progress answer for the active question, submitted with onNext
        val input: Answer? = null,
    ) {
        val activeQuestion: Question? get() = questions.firstOrNull { it.id == activeQuestionId }

        // Every question up to the first unanswered one, in order. Later questions stay hidden.
        // Questions don't disappear while an earlier answer is edited.
        val visibleQuestions: List<Question>
            get() {
                val firstUnanswered = questions.indexOfFirst { it.id !in answers }
                return if (firstUnanswered == -1) questions else questions.take(firstUnanswered + 1)
            }

        val allAnswered: Boolean get() = answers.size == questions.size

        // Every question is answered and none is being edited
        val isComplete: Boolean get() = activeQuestionId == null && allAnswered

        val canContinue: Boolean
            get() = when (val input = input) {
                is Answer.MultiChoice -> input.optionIds.isNotEmpty()
                null -> false
                else -> true
            }

        // Makes a question active, starting from its current answer or the question's default
        internal fun activate(questionId: String?): State {
            val question = questions.firstOrNull { it.id == questionId }
            return copy(
                activeQuestionId = questionId,
                input = answers[questionId] ?: (question as? Question.Number)?.let { Answer.Number(it.default) },
            )
        }
    }
}
