package com.everlog.ui.activities.questionform

import com.everlog.testutil.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuestionFormViewModelTest {

    private val yesNo = Question.YesNo(id = "yesNo", title = "Yes or no?")
    private val number = Question.Number(id = "number", title = "How many?", min = 1, max = 7)
    private val single = Question.SingleChoice(
        id = "single",
        title = "Pick one",
        options = listOf(Question.Option("a", "A"), Question.Option("b", "B")),
    )
    private val multi = Question.MultiChoice(
        id = "multi",
        title = "Pick some",
        options = listOf(Question.Option("x", "X"), Question.Option("y", "Y")),
    )

    private fun viewModel() = QuestionFormViewModel(
        dispatcher = FakeCoroutineDispatcher,
        questions = listOf(yesNo, number, single, multi),
    )

    private fun QuestionFormViewModel.answerAll() {
        onYesNoAnswer(yesNo.id, true)
        onNumberInputChange(number.id, "3")
        onContinue(number.id)
        onOptionSelect(single.id, "a")
        onOptionToggle(multi.id, "x")
        onContinue(multi.id)
    }

    @Test
    fun `starts with only the first question`() {
        val state = viewModel().state.value

        assertThat(state.activeQuestionId).isEqualTo(yesNo.id)
        assertThat(state.visibleQuestions).containsExactly(yesNo)
        assertThat(state.isComplete).isFalse()
    }

    @Test
    fun `answering reveals the next question`() {
        val viewModel = viewModel()

        viewModel.onYesNoAnswer(yesNo.id, false)

        val state = viewModel.state.value
        assertThat(state.answers).containsExactly(yesNo.id, Answer.YesNo(false))
        assertThat(state.activeQuestionId).isEqualTo(number.id)
        assertThat(state.visibleQuestions).containsExactly(yesNo, number).inOrder()
    }

    @Test
    fun `number needs a value in range to continue`() {
        val viewModel = viewModel()
        viewModel.onYesNoAnswer(yesNo.id, true)

        viewModel.onNumberInputChange(number.id, "9")
        viewModel.onContinue(number.id)

        assertThat(viewModel.state.value.canContinue).isFalse()
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(number.id)

        viewModel.onNumberInputChange(number.id, "4")
        viewModel.onContinue(number.id)

        assertThat(viewModel.state.value.answers[number.id]).isEqualTo(Answer.Number(4))
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(single.id)
    }

    @Test
    fun `number input drops non digits`() {
        val viewModel = viewModel()
        viewModel.onYesNoAnswer(yesNo.id, true)

        viewModel.onNumberInputChange(number.id, "3a.")

        assertThat(viewModel.state.value.numberInput).isEqualTo("3")
    }

    @Test
    fun `multiple choice needs a selection to continue`() {
        val viewModel = viewModel()
        viewModel.onYesNoAnswer(yesNo.id, true)
        viewModel.onNumberInputChange(number.id, "3")
        viewModel.onContinue(number.id)
        viewModel.onOptionSelect(single.id, "b")

        viewModel.onContinue(multi.id)
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(multi.id)

        viewModel.onOptionToggle(multi.id, "x")
        viewModel.onOptionToggle(multi.id, "y")
        viewModel.onOptionToggle(multi.id, "x")
        viewModel.onContinue(multi.id)

        assertThat(viewModel.state.value.answers[multi.id]).isEqualTo(Answer.MultiChoice(setOf("y")))
    }

    @Test
    fun `completes once every question is answered`() {
        val viewModel = viewModel()

        viewModel.answerAll()

        val state = viewModel.state.value
        assertThat(state.isComplete).isTrue()
        assertThat(state.activeQuestionId).isNull()
        assertThat(state.visibleQuestions).containsExactly(yesNo, number, single, multi).inOrder()
    }

    @Test
    fun `editing pre-fills the current answer`() {
        val viewModel = viewModel()
        viewModel.answerAll()

        viewModel.onEdit(number.id)
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(number.id)
        assertThat(viewModel.state.value.numberInput).isEqualTo("3")

        viewModel.onEdit(multi.id)
        assertThat(viewModel.state.value.multiChoiceInput).containsExactly("x")
    }

    @Test
    fun `changing an earlier answer keeps the later ones`() {
        val viewModel = viewModel()
        viewModel.answerAll()

        viewModel.onEdit(yesNo.id)
        viewModel.onYesNoAnswer(yesNo.id, false)

        val state = viewModel.state.value
        assertThat(state.answers[yesNo.id]).isEqualTo(Answer.YesNo(false))
        assertThat(state.answers[single.id]).isEqualTo(Answer.SingleChoice("a"))
        assertThat(state.isComplete).isTrue()
    }

    @Test
    fun `after editing it moves on to the first unanswered question`() {
        val viewModel = viewModel()
        viewModel.onYesNoAnswer(yesNo.id, true)
        viewModel.onNumberInputChange(number.id, "3")
        viewModel.onContinue(number.id)

        viewModel.onEdit(yesNo.id)
        assertThat(viewModel.state.value.visibleQuestions).containsExactly(yesNo, number).inOrder()

        viewModel.onYesNoAnswer(yesNo.id, false)
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(single.id)
    }

    @Test
    fun `ignores input for questions that are not active`() {
        val viewModel = viewModel()

        viewModel.onOptionSelect(single.id, "a")
        viewModel.onNumberInputChange(number.id, "3")

        assertThat(viewModel.state.value.answers).isEmpty()
        assertThat(viewModel.state.value.numberInput).isEmpty()
    }

    @Test
    fun `restart clears the answers`() {
        val viewModel = viewModel()
        viewModel.answerAll()

        viewModel.onRestart()

        assertThat(viewModel.state.value.answers).isEmpty()
        assertThat(viewModel.state.value.activeQuestionId).isEqualTo(yesNo.id)
    }
}
