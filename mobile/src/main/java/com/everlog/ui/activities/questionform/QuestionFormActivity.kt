package com.everlog.ui.activities.questionform

import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers

// Debug-only prototype of a one-question-at-a-time form, opened from Settings. Copy is hardcoded and
// the private composables below are placeholders for design system elements (see TAS-441).
class QuestionFormActivity : CommonComposeActivity() {
    private val viewModel by viewModels<QuestionFormViewModel> {
        createViewModelFactory { QuestionFormViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = QuestionFormScreen(viewModel = viewModel)
}

@Composable
internal fun QuestionFormScreen(viewModel: QuestionFormViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    QuestionFormState(
        state = state,
        onYesNoAnswer = viewModel::onYesNoAnswer,
        onOptionSelect = viewModel::onOptionSelect,
        onOptionToggle = viewModel::onOptionToggle,
        onNumberInputChange = viewModel::onNumberInputChange,
        onContinue = viewModel::onContinue,
        onEdit = viewModel::onEdit,
        onRestart = viewModel::onRestart,
    )
}

@Composable
private fun QuestionFormState(
    state: QuestionFormViewModel.State,
    onYesNoAnswer: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberInputChange: (questionId: String, input: String) -> Unit,
    onContinue: (questionId: String) -> Unit,
    onEdit: (questionId: String) -> Unit,
    onRestart: () -> Unit,
) {
    val listState = rememberLazyListState()
    val visibleQuestions = state.visibleQuestions

    // Keep the question being answered (or the completion message) in view
    LaunchedEffect(state.activeQuestionId, state.isComplete) {
        val index = visibleQuestions.indexOfFirst { it.id == state.activeQuestionId }
            .takeIf { it >= 0 }
            ?: visibleQuestions.size
        listState.animateScrollToItem(index)
    }

    AppScreen(
        topBar = {
            AppBar(
                title = "Question form",
                showBack = true,
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = visibleQuestions,
                key = { it.id }
            ) { question ->
                val position = state.questions.indexOf(question) + 1
                Column(modifier = Modifier.animateItem()) {
                    if (question.id == state.activeQuestionId) {
                        ActiveQuestion(
                            question = question,
                            position = position,
                            total = state.questions.size,
                            state = state,
                            onYesNoAnswer = onYesNoAnswer,
                            onOptionSelect = onOptionSelect,
                            onOptionToggle = onOptionToggle,
                            onNumberInputChange = onNumberInputChange,
                            onContinue = onContinue,
                        )
                    } else {
                        AnsweredQuestion(
                            question = question,
                            answer = state.answers[question.id],
                            onEdit = { onEdit(question.id) },
                        )
                    }
                }
            }
            if (state.isComplete) {
                item(key = "complete") {
                    Completion(
                        modifier = Modifier.animateItem(),
                        onRestart = onRestart,
                    )
                }
            }
        }
    }
}

// Element: collapsed answered question, tappable to edit
@Composable
private fun AnsweredQuestion(
    question: Question,
    answer: Answer?,
    onEdit: () -> Unit,
) {
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .clickable(onClick = onEdit)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppText(
                    text = question.title,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
                AppText(
                    text = answer?.label(question) ?: "",
                    style = Theme.typography.bodyBold,
                )
            }
            AppText(
                text = "Edit",
                style = Theme.typography.button,
                color = Theme.contentColors.action,
            )
        }
    }
}

// Element: expanded question card with its progress, title and input
@Composable
private fun ActiveQuestion(
    question: Question,
    position: Int,
    total: Int,
    state: QuestionFormViewModel.State,
    onYesNoAnswer: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberInputChange: (questionId: String, input: String) -> Unit,
    onContinue: (questionId: String) -> Unit,
) {
    val answer = state.answers[question.id]

    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(
                    text = "Question $position of $total",
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
                AppText(
                    text = question.title,
                    style = Theme.typography.heading,
                )
            }
            when (question) {
                is Question.YesNo -> YesNoInput(
                    selected = (answer as? Answer.YesNo)?.value,
                    onAnswer = { onYesNoAnswer(question.id, it) },
                )

                is Question.SingleChoice -> Column {
                    question.options.forEach { option ->
                        ChoiceRow(
                            label = option.label,
                            selected = (answer as? Answer.SingleChoice)?.optionId == option.id,
                            multiple = false,
                            onClick = { onOptionSelect(question.id, option.id) },
                        )
                    }
                }

                is Question.MultiChoice -> {
                    Column {
                        question.options.forEach { option ->
                            ChoiceRow(
                                label = option.label,
                                selected = option.id in state.multiChoiceInput,
                                multiple = true,
                                onClick = { onOptionToggle(question.id, option.id) },
                            )
                        }
                    }
                    ContinueButton(
                        enabled = state.canContinue,
                        onClick = { onContinue(question.id) },
                    )
                }

                is Question.Number -> {
                    NumberInput(
                        value = state.numberInput,
                        unit = question.unit,
                        hint = "Between ${question.min} and ${question.max}",
                        onValueChange = { onNumberInputChange(question.id, it) },
                        onDone = { onContinue(question.id) },
                    )
                    ContinueButton(
                        enabled = state.canContinue,
                        onClick = { onContinue(question.id) },
                    )
                }
            }
        }
    }
}

// Element: yes/no choice. Shows the current answer as the filled button when editing.
@Composable
private fun YesNoInput(
    selected: Boolean?,
    onAnswer: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true to "Yes", false to "No").forEach { (value, label) ->
            if (selected == value) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onAnswer(value) },
                    text = label,
                )
            } else {
                AppSecondaryButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onAnswer(value) },
                    text = label,
                )
            }
        }
    }
}

// Element: single (radio) or multiple (checkbox) choice row
@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiple) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Theme.contentColors.action,
                    uncheckedColor = Theme.contentColors.secondary,
                    checkmarkColor = Theme.contentColors.onAction,
                ),
            )
        } else {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Theme.contentColors.action,
                    unselectedColor = Theme.contentColors.secondary,
                ),
            )
        }
        AppText(
            modifier = Modifier.weight(1f),
            text = label,
        )
    }
}

// Element: number text field with a unit and a hint
@Composable
private fun NumberInput(
    value: String,
    unit: String?,
    hint: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = Theme.typography.body,
        suffix = unit?.let { { AppText(text = it, color = Theme.contentColors.secondary) } },
        supportingText = { AppText(text = hint, style = Theme.typography.small, color = Theme.contentColors.secondary) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Theme.contentColors.primary,
            unfocusedTextColor = Theme.contentColors.primary,
            focusedBorderColor = Theme.contentColors.action,
            unfocusedBorderColor = Theme.contentColors.secondary,
            cursorColor = Theme.contentColors.action,
        ),
    )
}

@Composable
private fun ContinueButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        text = "Continue",
        enabled = enabled,
    )
}

// Element: end of form state
@Composable
private fun Completion(
    modifier: Modifier = Modifier,
    onRestart: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = "All done. Tap an answer to change it.",
            style = Theme.typography.caption,
            color = Theme.contentColors.secondary,
        )
        AppSecondaryButton(
            onClick = onRestart,
            text = "Start over",
        )
    }
}

private fun Answer.label(question: Question): String = when (this) {
    is Answer.YesNo -> if (value) "Yes" else "No"
    is Answer.Number -> listOfNotNull(value.toString(), (question as? Question.Number)?.unit).joinToString(" ")
    is Answer.SingleChoice -> question.optionLabels(setOf(optionId))
    is Answer.MultiChoice -> question.optionLabels(optionIds)
}

private fun Question.optionLabels(optionIds: Set<String>): String {
    val options = when (this) {
        is Question.SingleChoice -> options
        is Question.MultiChoice -> options
        is Question.YesNo, is Question.Number -> emptyList()
    }
    return options.filter { it.id in optionIds }.joinToString(", ") { it.label }
}

@Preview
@Composable
private fun QuestionFormPreview() {
    Theme {
        QuestionFormState(
            state = QuestionFormViewModel.State(
                questions = SampleQuestions,
                answers = mapOf("experience" to Answer.YesNo(true)),
                activeQuestionId = "days",
                numberInput = "3",
            ),
            onYesNoAnswer = { _, _ -> },
            onOptionSelect = { _, _ -> },
            onOptionToggle = { _, _ -> },
            onNumberInputChange = { _, _ -> },
            onContinue = {},
            onEdit = {},
            onRestart = {},
        )
    }
}
