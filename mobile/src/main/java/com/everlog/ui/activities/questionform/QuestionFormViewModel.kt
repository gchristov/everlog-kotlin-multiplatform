package com.everlog.ui.activities.questionform

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Asks [Question]s one at a time. Answering the active question moves on to the first unanswered
 * one, and answered questions can be reopened and changed without losing the answers after them.
 */
class QuestionFormViewModel(
    dispatcher: CoroutineDispatcher,
    questions: List<Question> = SampleQuestions,
) : CommonViewModel<QuestionFormViewModel.State>(
    dispatcher = dispatcher,
    initialState = State(
        questions = questions,
        activeQuestionId = questions.firstOrNull()?.id,
    )
) {
    fun onYesNoAnswer(questionId: String, value: Boolean) {
        submit(questionId, Answer.YesNo(value))
    }

    fun onOptionSelect(questionId: String, optionId: String) {
        submit(questionId, Answer.SingleChoice(optionId))
    }

    fun onOptionToggle(questionId: String, optionId: String) {
        if (questionId != state.value.activeQuestionId) return
        setState {
            copy(
                multiChoiceInput = if (optionId in multiChoiceInput) {
                    multiChoiceInput - optionId
                } else {
                    multiChoiceInput + optionId
                }
            )
        }
    }

    fun onNumberInputChange(questionId: String, input: String) {
        if (questionId != state.value.activeQuestionId) return
        setState { copy(numberInput = input.filter { it.isDigit() }.take(MaxNumberLength)) }
    }

    // Submits the input for questions that need more than one tap (number and multiple choice)
    fun onContinue(questionId: String) {
        val currentState = state.value
        val question = currentState.activeQuestion ?: return
        if (question.id != questionId || !currentState.canContinue) return
        val answer = when (question) {
            is Question.Number -> Answer.Number(currentState.numberInput.toInt())
            is Question.MultiChoice -> Answer.MultiChoice(currentState.multiChoiceInput)
            is Question.YesNo, is Question.SingleChoice -> return
        }
        submit(questionId, answer)
    }

    fun onEdit(questionId: String) {
        setState { activate(questionId) }
    }

    fun onRestart() {
        setState {
            State(
                questions = questions,
                activeQuestionId = questions.firstOrNull()?.id,
            )
        }
    }

    private fun submit(questionId: String, answer: Answer) {
        if (questionId != state.value.activeQuestionId) return
        setState {
            val newAnswers = answers + (questionId to answer)
            val nextQuestionId = questions.firstOrNull { it.id !in newAnswers }?.id
            copy(answers = newAnswers).activate(nextQuestionId)
        }
    }

    // Makes a question active, pre-filling its input with the current answer
    private fun State.activate(questionId: String?): State {
        val answer = questionId?.let { answers[it] }
        return copy(
            activeQuestionId = questionId,
            numberInput = (answer as? Answer.Number)?.value?.toString() ?: "",
            multiChoiceInput = (answer as? Answer.MultiChoice)?.optionIds ?: emptySet(),
        )
    }

    data class State(
        val questions: List<Question>,
        val answers: Map<String, Answer> = emptyMap(),
        // Null once every question is answered and none is being edited
        val activeQuestionId: String?,
        // In-progress input for the active question
        val numberInput: String = "",
        val multiChoiceInput: Set<String> = emptySet(),
    ) {
        val activeQuestion: Question? get() = questions.firstOrNull { it.id == activeQuestionId }

        // Answered questions and the active one, in order. Later questions stay hidden.
        val visibleQuestions: List<Question>
            get() = questions.filter { it.id in answers || it.id == activeQuestionId }

        val isComplete: Boolean get() = activeQuestionId == null && answers.size == questions.size

        val canContinue: Boolean
            get() = when (val question = activeQuestion) {
                is Question.Number -> numberInput.toIntOrNull()?.let { it in question.min..question.max } == true
                is Question.MultiChoice -> multiChoiceInput.isNotEmpty()
                is Question.YesNo, is Question.SingleChoice, null -> false
            }
    }
}

private const val MaxNumberLength = 4
