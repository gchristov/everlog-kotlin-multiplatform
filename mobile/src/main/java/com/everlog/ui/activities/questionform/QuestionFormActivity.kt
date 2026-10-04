package com.everlog.ui.activities.questionform

import android.widget.Toast
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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.alpha
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
import com.everlog.utils.format.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

// Debug-only prototype of a one-question-at-a-time form for the app's settings, opened from Settings.
// Copy is hardcoded and the private composables below are placeholders for design system elements
// (see TAS-440).
class QuestionFormActivity : CommonComposeActivity() {
    private val viewModel by viewModels<QuestionFormViewModel> {
        createViewModelFactory {
            // The view-model outlives the activity, so it only holds on to the application context
            val appContext = applicationContext
            QuestionFormViewModel(
                dispatcher = Dispatchers.Main,
                questions = SettingsQuestions.build(appContext),
                saveAnswers = { answers -> SettingsQuestions.save(appContext, answers) },
            )
        }
    }

    @Composable
    override fun Content() = QuestionFormScreen(
        viewModel = viewModel,
        onSaved = {
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    )
}

@Composable
internal fun QuestionFormScreen(
    viewModel: QuestionFormViewModel,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    QuestionFormState(
        state = state,
        onYesNoSelect = viewModel::onYesNoSelect,
        onOptionSelect = viewModel::onOptionSelect,
        onOptionToggle = viewModel::onOptionToggle,
        onNumberChange = viewModel::onNumberChange,
        onNext = viewModel::onNext,
        onEdit = viewModel::onEdit,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun QuestionFormState(
    state: QuestionFormViewModel.State,
    onYesNoSelect: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberChange: (questionId: String, value: Double) -> Unit,
    onNext: () -> Unit,
    onEdit: (questionId: String) -> Unit,
    onSave: () -> Unit,
) {
    val listState = rememberLazyListState()
    val visibleQuestions = state.visibleQuestions

    // Keep the question being answered (or the completion message) in view
    LaunchedEffect(state.activeQuestionId, state.isComplete) {
        val index = visibleQuestions.indexOfFirst { it.id == state.activeQuestionId }
            .takeIf { it >= 0 }
            ?: visibleQuestions.size
        // Wait for the cards to finish resizing, so the list doesn't scroll while they animate
        delay(QuestionTransitionMillis.toLong())
        listState.scrollIntoView(index)
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
                            text = "Save",
                            onClick = onSave,
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
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = visibleQuestions,
                key = { it.id }
            ) { question ->
                AnimatedContent(
                    // No placement animation: cards below follow the resizing card frame by frame,
                    // instead of lagging behind and overlapping it
                    modifier = Modifier.animateItem(placementSpec = null),
                    // Carries the input, so a closing question keeps showing its selection while it
                    // fades out. Only opening and closing animate, not input changes.
                    targetState = QuestionItem(
                        active = question.id == state.activeQuestionId,
                        input = state.input.takeIf { question.id == state.activeQuestionId },
                    ),
                    contentKey = { it.active },
                    // Fade through: the old content fades out quickly, then the new one fades in while
                    // the card resizes. Clipped, so a growing card doesn't draw over the one below it.
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = FadeInMillis, delayMillis = FadeOutMillis)) togetherWith
                                fadeOut(tween(durationMillis = FadeOutMillis)))
                            .using(SizeTransform(clip = true) { _, _ -> tween(QuestionTransitionMillis) })
                    },
                    label = "question",
                ) { item ->
                    if (item.active) {
                        ActiveQuestion(
                            question = question,
                            position = state.questions.indexOf(question) + 1,
                            total = state.questions.size,
                            input = item.input,
                            answers = state.answers,
                            onYesNoSelect = onYesNoSelect,
                            onOptionSelect = onOptionSelect,
                            onOptionToggle = onOptionToggle,
                            onNumberChange = onNumberChange,
                        )
                    } else {
                        CollapsedQuestion(
                            question = question,
                            answer = state.answers[question.id],
                            answers = state.answers,
                            onClick = { onEdit(question.id) },
                        )
                    }
                }
            }
            // Stays while an answer is edited, so nothing disappears from under the open question
            if (state.allAnswered) {
                item(key = "complete") {
                    AppText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(placementSpec = null),
                        text = "All set. Tap an answer to change it, or save.",
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// Element: collapsed question, showing its answer (tap to edit) or that it's still to answer
@Composable
private fun CollapsedQuestion(
    question: Question,
    answer: Answer?,
    answers: Map<String, Answer>,
    onClick: () -> Unit,
) {
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .clickable(onClick = onClick)
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
                    text = answer?.label(question, answers) ?: "Not answered yet",
                    style = Theme.typography.bodyBold,
                    color = if (answer != null) Theme.contentColors.primary else Theme.contentColors.secondary,
                )
            }
            AppText(
                text = if (answer != null) "Edit" else "Answer",
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
    answers: Map<String, Answer>,
    onYesNoSelect: (questionId: String, value: Boolean) -> Unit,
    onOptionSelect: (questionId: String, optionId: String) -> Unit,
    onOptionToggle: (questionId: String, optionId: String) -> Unit,
    onNumberChange: (questionId: String, value: Double) -> Unit,
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
                            option = option,
                            selected = (input as? Answer.SingleChoice)?.optionId == option.id,
                            multiple = false,
                            onClick = { onOptionSelect(question.id, option.id) },
                        )
                    }
                }

                is Question.MultiChoice -> Column {
                    question.options.forEach { option ->
                        ChoiceRow(
                            option = option,
                            selected = option.id in ((input as? Answer.MultiChoice)?.optionIds ?: emptySet()),
                            multiple = true,
                            onClick = { onOptionToggle(question.id, option.id) },
                        )
                    }
                }

                is Question.Number -> NumberStepper(
                    value = (input as? Answer.Number)?.value ?: question.min,
                    question = question,
                    unit = question.unit(answers),
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
    option: Question.Option,
    selected: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(Theme.shapes.surface)
            .clickable(enabled = option.enabled, onClick = onClick)
            .alpha(if (option.enabled) 1f else 0.4f),
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            AppText(text = option.label)
            option.description?.let { description ->
                AppText(
                    text = description,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

// Element: −/+ stepper for picking a number in a range
@Composable
private fun NumberStepper(
    value: Double,
    question: Question.Number,
    unit: String?,
    onValueChange: (Double) -> Unit,
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
                text = value.format(),
                style = Theme.typography.title,
            )
            unit?.let { unit ->
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

private data class QuestionItem(
    val active: Boolean,
    val input: Answer?,
)

private const val QuestionTransitionMillis = 300
private const val FadeOutMillis = 90
private const val FadeInMillis = QuestionTransitionMillis - FadeOutMillis

// Scrolls the least needed to show the item in full above the bottom bar, or not at all
private suspend fun LazyListState.scrollIntoView(index: Int) {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val visibleEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val delta = when {
        item.offset < 0 -> item.offset
        // Scroll up to show the bottom, but never past the item's top
        item.offset + item.size > visibleEnd -> minOf(item.offset + item.size - visibleEnd, item.offset)
        else -> 0
    }
    if (delta != 0) {
        animateScrollBy(delta.toFloat())
    }
}

// Whole numbers without decimals, and others the way the app shows weights (e.g. 2.5)
private fun Double.format(): String = FormatUtils.formatSetWeight(toFloat())

private fun Answer.label(question: Question, answers: Map<String, Answer>): String = when (this) {
    is Answer.YesNo -> if (value) "Yes" else "No"
    is Answer.Number -> listOfNotNull(value.format(), (question as? Question.Number)?.unit?.invoke(answers)).joinToString(" ")
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
                questions = listOf(
                    Question.YesNo(id = "yesNo", title = "Yes or no?"),
                    Question.Number(id = "number", title = "How many?", min = 1.0, max = 7.0, unit = { "days" }),
                ),
                answers = mapOf("yesNo" to Answer.YesNo(true)),
                activeQuestionId = "number",
                input = Answer.Number(3.0),
            ),
            onYesNoSelect = { _, _ -> },
            onOptionSelect = { _, _ -> },
            onOptionToggle = { _, _ -> },
            onNumberChange = { _, _ -> },
            onNext = {},
            onEdit = {},
            onSave = {},
        )
    }
}
