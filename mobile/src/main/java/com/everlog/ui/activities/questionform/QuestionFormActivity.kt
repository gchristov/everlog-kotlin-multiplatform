package com.everlog.ui.activities.questionform

import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers

// Debug-only prototype of a one-question-at-a-time form, opened from Settings. Copy is hardcoded and
// the private composables below are placeholders for design system elements (see TAS-440).
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
        onYesNoSelect = viewModel::onYesNoSelect,
        onOptionSelect = viewModel::onOptionSelect,
        onOptionToggle = viewModel::onOptionToggle,
        onNumberChange = viewModel::onNumberChange,
        onNext = viewModel::onNext,
        onEdit = viewModel::onEdit,
        onRestart = viewModel::onRestart,
    )
}

@Composable
private fun QuestionFormState(
    state: QuestionFormViewModel.State,
    onYesNoSelect: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberChange: (questionId: String, value: Int) -> Unit,
    onNext: () -> Unit,
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
        },
        bottomBar = {
            AppFooter(
                actions = listOf(
                    if (state.isComplete) {
                        AppFooterAction(
                            text = "Start over",
                            onClick = onRestart,
                            style = AppFooterAction.Style.Secondary,
                        )
                    } else {
                        AppFooterAction(
                            text = "Next",
                            onClick = onNext,
                            enabled = state.canContinue,
                        )
                    }
                )
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = visibleQuestions,
                key = { it.id }
            ) { question ->
                AnimatedContent(
                    modifier = Modifier.animateItem(),
                    targetState = question.id == state.activeQuestionId,
                    // Cross-fade between the expanded and collapsed question while the card resizes
                    transitionSpec = {
                        (fadeIn(tween(QuestionTransitionMillis)) togetherWith
                                fadeOut(tween(QuestionTransitionMillis)))
                            .using(SizeTransform(clip = false) { _, _ -> tween(QuestionTransitionMillis) })
                    },
                    label = "question",
                ) { active ->
                    if (active) {
                        ActiveQuestion(
                            question = question,
                            position = state.questions.indexOf(question) + 1,
                            total = state.questions.size,
                            input = state.input,
                            onYesNoSelect = onYesNoSelect,
                            onOptionSelect = onOptionSelect,
                            onOptionToggle = onOptionToggle,
                            onNumberChange = onNumberChange,
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
                    AppText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        text = "All done. Tap an answer to change it.",
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                        textAlign = TextAlign.Center,
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
    input: Answer?,
    onYesNoSelect: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberChange: (questionId: String, value: Int) -> Unit,
) {
    AppSurface(modifier = Modifier.fillMaxWidth()) {
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
                    selected = (input as? Answer.YesNo)?.value,
                    onSelect = { onYesNoSelect(question.id, it) },
                )

                is Question.SingleChoice -> Column {
                    question.options.forEach { option ->
                        ChoiceRow(
                            label = option.label,
                            selected = (input as? Answer.SingleChoice)?.optionId == option.id,
                            multiple = false,
                            onClick = { onOptionSelect(question.id, option.id) },
                        )
                    }
                }

                is Question.MultiChoice -> Column {
                    question.options.forEach { option ->
                        ChoiceRow(
                            label = option.label,
                            selected = option.id in ((input as? Answer.MultiChoice)?.optionIds ?: emptySet()),
                            multiple = true,
                            onClick = { onOptionToggle(question.id, option.id) },
                        )
                    }
                }

                is Question.Number -> NumberStepper(
                    value = (input as? Answer.Number)?.value ?: question.default,
                    question = question,
                    onValueChange = { onNumberChange(question.id, it) },
                )
            }
        }
    }
}

// Element: yes/no choice, with the selected answer filled
@Composable
private fun YesNoInput(
    selected: Boolean?,
    onSelect: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true to "Yes", false to "No").forEach { (value, label) ->
            if (selected == value) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(value) },
                    text = label,
                )
            } else {
                AppSecondaryButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(value) },
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
            .heightIn(min = 48.dp)
            .clip(Theme.shapes.surface)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

// Element: −/+ stepper for picking a number in a range
@Composable
private fun NumberStepper(
    value: Int,
    question: Question.Number,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepperButton(
            icon = ImageVector.vectorResource(R.drawable.ic_remove_white),
            contentDescription = "Decrease",
            enabled = value > question.min,
            onClick = { onValueChange(value - question.step) },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(
                text = value.toString(),
                style = Theme.typography.title,
            )
            question.unit?.let { unit ->
                AppText(
                    text = unit,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
        }
        StepperButton(
            icon = ImageVector.vectorResource(R.drawable.ic_add),
            contentDescription = "Increase",
            enabled = value < question.max,
            onClick = { onValueChange(value + question.step) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = Theme.contentColors.action
    AppIconButton(
        modifier = Modifier
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = if (enabled) color else color.copy(alpha = 0.4f),
                shape = CircleShape,
            ),
        onClick = onClick,
        icon = icon,
        tint = color,
        contentDescription = contentDescription,
        enabled = enabled,
    )
}

private const val QuestionTransitionMillis = 250

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
                input = Answer.Number(3),
            ),
            onYesNoSelect = { _, _ -> },
            onOptionSelect = { _, _ -> },
            onOptionToggle = { _, _ -> },
            onNumberChange = { _, _ -> },
            onNext = {},
            onEdit = {},
            onRestart = {},
        )
    }
}
