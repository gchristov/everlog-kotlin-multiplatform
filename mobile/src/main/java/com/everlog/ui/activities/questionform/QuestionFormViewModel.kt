package com.everlog.ui.activities.questionform

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.math.roundToInt

/**
 * Asks [Question]s one at a time. The active question's input is submitted with [onNext], which
 * moves on to the first unanswered question. Answered questions can be reopened and changed without
 * losing the answers after them, and [onSave] hands every answer to [saveAnswers].
 */
class QuestionFormViewModel(
    dispatcher: CoroutineDispatcher,
    questions: List<Question>,
    private val saveAnswers: (Map<String, Answer>) -> Unit,
) : CommonViewModel<QuestionFormViewModel.State>(
    dispatcher = dispatcher,
    initialState = State(questions = questions, activeQuestionId = null).activate(questions.firstOrNull()?.id)
) {
    fun onYesNoSelect(questionId: String, value: Boolean) {
        updateInput(questionId) { Answer.YesNo(value) }
    }

    fun onOptionSelect(questionId: String, optionId: String) {
        if (!isEnabledOption(optionId)) return
        updateInput(questionId) { Answer.SingleChoice(optionId) }
    }

    fun onOptionToggle(questionId: String, optionId: String) {
        if (!isEnabledOption(optionId)) return
        updateInput(questionId) { input ->
            val selected = (input as? Answer.MultiChoice)?.optionIds ?: emptySet()
            Answer.MultiChoice(if (optionId in selected) selected - optionId else selected + optionId)
        }
    }

    fun onNumberChange(questionId: String, value: Double) {
        val question = state.value.activeQuestion as? Question.Number ?: return
        // Snap to the question's step, avoiding floating point drift (e.g. 1.2499999)
        val steps = ((value - question.min) / question.step).roundToInt()
        val snapped = (question.min + steps * question.step).coerceIn(question.min, question.max)
        updateInput(questionId) { Answer.Number(snapped) }
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

    fun onSave() {
        val currentState = state.value
        if (!currentState.isComplete || currentState.saved) return
        saveAnswers(currentState.answers)
        setState { copy(saved = true) }
    }

    private fun isEnabledOption(optionId: String): Boolean {
        val options = when (val question = state.value.activeQuestion) {
            is Question.SingleChoice -> question.options
            is Question.MultiChoice -> question.options
            else -> return false
        }
        return options.any { it.id == optionId && it.enabled }
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
        val saved: Boolean = false,
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
                input = answers[questionId] ?: question?.default,
            )
        }
    }
}
