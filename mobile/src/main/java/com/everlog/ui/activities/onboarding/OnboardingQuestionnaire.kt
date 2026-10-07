package com.everlog.ui.activities.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay
import org.threeten.bp.DayOfWeek

// The setup questions: one open at a time, answered ones collapse to summary rows that can be
// reopened, and Build my week finishes. The content of OnboardingScreen's AppScreen, after the
// welcome.
@Composable
internal fun OnboardingQuestionnaire(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
    contentPadding: PaddingValues,
) {
    val scrollState = rememberScrollState()
    var viewportHeight by remember { mutableFloatStateOf(0f) }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    val questionTops = remember { mutableStateMapOf<String, Float>() }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // Keep the open question in view, once the collapsing question has given up its space
    LaunchedEffect(state.activeQuestionId) {
        val questionId = state.activeQuestionId ?: return@LaunchedEffect
        delay(OnboardingMotion.Collapse.toLong())
        val top = (questionTops[questionId] ?: return@LaunchedEffect) - viewportTop
        val delta = if (state.isEditing) {
            // Only when the question would sit under the top bar
            if (top < 0f) top else 0f
        } else {
            // The new question's top settles a third of the way down, never scrolling back up
            (top - viewportHeight / 3f).coerceAtLeast(0f)
        }
        if (delta != 0f) {
            scrollState.animateScrollBy(delta, tween(OnboardingMotion.Scroll, easing = OnboardingMotion.Standard))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportHeight = it.height.toFloat() }
            .onGloballyPositioned { viewportTop = it.positionInRoot().y }
    ) {
        SetupQuestions(
            state = state,
            viewModel = viewModel,
            scrollState = scrollState,
            viewportHeight = viewportHeight,
            contentPadding = contentPadding,
            onQuestionPositioned = { id, top -> questionTops[id] = top },
            onTimeClick = { showTimePicker = true },
        )
    }

    val reminders = state.input as? Answer.Reminders
    if (showTimePicker && reminders != null) {
        ReminderTimeDialog(
            hour = reminders.hour,
            minute = reminders.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.onReminderTimeChange(OnboardingQuestions.Reminders, hour, minute)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun SetupQuestions(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
    scrollState: ScrollState,
    viewportHeight: Float,
    contentPadding: PaddingValues,
    onQuestionPositioned: (id: String, top: Float) -> Unit,
    onTimeClick: () -> Unit,
) {
    val density = LocalDensity.current
    val enterOffset = with(density) { OnboardingMotion.EnterOffset.roundToPx() }
    val visibleIds = state.visibleQuestions.map { it.id }
    val groupPositions = groupPositions(visibleIds, state.activeQuestionId)
    // The first question sits lower on an empty screen, like the design's first state
    val topPadding by animateDpAsState(
        targetValue = if (state.answers.isEmpty()) Theme.spacing.extraLarge else Theme.spacing.large,
        animationSpec = tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard),
        label = "topPadding",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = Theme.spacing.large, end = Theme.spacing.large, top = topPadding),
    ) {
        state.questions.forEach { question ->
            AnimatedVisibility(
                modifier = Modifier.onGloballyPositioned { onQuestionPositioned(question.id, it.positionInRoot().y) },
                visible = question.id in visibleIds,
                // A new question fades in and slides up as the previous one collapses
                enter = fadeIn(tween(OnboardingMotion.Enter, OnboardingMotion.AppendDelay, OnboardingMotion.EmphasizedDecelerate)) +
                        slideInVertically(tween(OnboardingMotion.Enter, OnboardingMotion.AppendDelay, OnboardingMotion.EmphasizedDecelerate)) { enterOffset },
                exit = fadeOut(tween(OnboardingMotion.Exit)),
            ) {
                QuestionItem(
                    question = question,
                    state = state,
                    position = groupPositions[question.id],
                    viewModel = viewModel,
                    onTimeClick = onTimeClick,
                )
            }
        }
        // Room for the last questions to reach the upper third, above the footer
        VerticalSpace(height = with(density) { (viewportHeight / 2f).toDp() } + contentPadding.calculateBottomPadding())
    }
}

// Each closed question's place in its run of rows, as the open question splits them into groups
private fun groupPositions(visibleIds: List<String>, openId: String?): Map<String, GroupPosition> {
    val runs = mutableListOf(mutableListOf<String>())
    visibleIds.forEach { id ->
        if (id == openId) runs += mutableListOf<String>() else runs.last() += id
    }
    return runs.flatMap { run -> run.mapIndexed { index, id -> id to GroupPosition(index, run.size) } }.toMap()
}

private enum class ItemMode {
    Open,
    Answered,
    UpNext,
}

private data class ItemState(
    val mode: ItemMode,
    // Kept with the target state, so a closing question keeps its selection while it fades out
    val input: Answer?,
    val editing: Boolean,
)

@Composable
private fun QuestionItem(
    question: OnboardingQuestion,
    state: OnboardingViewModel.State,
    // In its group of rows, while it's closed
    position: GroupPosition?,
    viewModel: OnboardingViewModel,
    onTimeClick: () -> Unit,
) {
    val open = question.id == state.activeQuestionId
    val itemState = ItemState(
        mode = when {
            open -> ItemMode.Open
            question.id in state.answers -> ItemMode.Answered
            else -> ItemMode.UpNext
        },
        input = state.input.takeIf { open },
        editing = open && state.isEditing,
    )
    AnimatedContent(
        targetState = itemState,
        contentKey = { it.mode },
        transitionSpec = {
            val enter = if (targetState.mode == ItemMode.Open) {
                // Re-expanding: options fade in
                fadeIn(tween(OnboardingMotion.Expand, OnboardingMotion.ExpandDelay, OnboardingMotion.EmphasizedDecelerate))
            } else {
                // Collapsing: the summary row fades in once the options have gone
                fadeIn(tween(OnboardingMotion.SummaryFade, OnboardingMotion.SummaryDelay, OnboardingMotion.Standard))
            }
            (enter togetherWith fadeOut(tween(OnboardingMotion.FadeOut, easing = OnboardingMotion.EmphasizedAccelerate)))
                .using(SizeTransform(clip = true) { _, _ -> tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard) })
        },
        label = "question",
    ) { item ->
        when (item.mode) {
            // Apart from the rows around it like groups are, so it splits them into two
            ItemMode.Open -> Box(modifier = Modifier.padding(top = if (question.id == state.visibleQuestions.firstOrNull()?.id) 0.dp else Theme.spacing.extraLarge, bottom = Theme.spacing.extraLarge)) {
                QuestionBlock(
                    question = question,
                    input = item.input,
                    editing = item.editing,
                    viewModel = viewModel,
                    onTimeClick = onTimeClick,
                )
            }
            ItemMode.Answered -> SummaryRow(
                label = stringResource(question.label),
                answer = state.answers[question.id]?.let { answerSummary(question, it, state.firstDayOfWeek) }.orEmpty(),
                skipped = question.id in state.skipped,
                position = position ?: GroupPosition(0, 1),
                onClick = { viewModel.onEdit(question.id) },
            )
            ItemMode.UpNext -> UpNextRow(
                label = stringResource(question.label),
                position = position ?: GroupPosition(0, 1),
            )
        }
    }
}

@Composable
private fun QuestionBlock(
    question: OnboardingQuestion,
    input: Answer?,
    editing: Boolean,
    viewModel: OnboardingViewModel,
    onTimeClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (editing) {
            AppText(
                text = stringResource(R.string.onboarding_editing),
                style = Theme.typography.caption,
                color = Theme.contentColors.action,
            )
            VerticalSpace(Theme.spacing.small)
        }
        AppText(text = stringResource(question.title), style = Theme.typography.title)
        question.helper?.let { stringResource(it) }?.let { helper ->
            VerticalSpace(Theme.spacing.small)
            AppText(text = helper, style = Theme.typography.body, color = Theme.contentColors.secondary)
        }
        VerticalSpace(if (question is OnboardingQuestion.Choice && question.sideBySide) Theme.spacing.extraLarge else Theme.spacing.large)
        when (question) {
            is OnboardingQuestion.Choice -> ChoiceOptions(
                question = question,
                selectedId = (input as? Answer.Choice)?.optionId,
                onSelect = { viewModel.onOptionSelect(question.id, it) },
            )
            // Same squares and spacing as the reminder days
            is OnboardingQuestion.Days -> Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                question.options.forEach { count ->
                    NumberChip(
                        modifier = Modifier.weight(1f),
                        value = count,
                        description = pluralStringResource(question.answer, count, count),
                        selected = (input as? Answer.Days)?.count == count,
                        onClick = { viewModel.onDaysSelect(question.id, count) },
                    )
                }
            }
            is OnboardingQuestion.Reminders -> RemindersInput(
                reminders = input as? Answer.Reminders,
                firstDayOfWeek = viewModel.state.value.firstDayOfWeek,
                onDayToggle = { viewModel.onReminderDayToggle(question.id, it) },
                onTimeClick = onTimeClick,
            )
        }
    }
}

@Composable
private fun ChoiceOptions(
    question: OnboardingQuestion.Choice,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    if (question.sideBySide) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
            question.options.forEach { option ->
                UnitCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(option.title),
                    description = option.description?.let { stringResource(it) }.orEmpty(),
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
            question.options.forEach { option ->
                SelectableListItem(
                    title = stringResource(option.title),
                    description = option.description?.let { stringResource(it) },
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }
}

@Composable
private fun RemindersInput(
    reminders: Answer.Reminders?,
    firstDayOfWeek: DayOfWeek,
    onDayToggle: (DayOfWeek) -> Unit,
    onTimeClick: () -> Unit,
) {
    val days = reminders?.days.orEmpty()
    Column {
        // Starting from the first day of the week set in Settings
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            OnboardingQuestions.week(startingOn = firstDayOfWeek).forEach { day ->
                DayChip(
                    modifier = Modifier.weight(1f),
                    label = day.shortName(),
                    fullLabel = day.fullName(),
                    selected = day in days,
                    onClick = { onDayToggle(day) },
                )
            }
        }
        VerticalSpace(Theme.spacing.large)
        AppListGroup(footer = stringResource(R.string.onboarding_reminders_permission)) {
            row {
                TimeRow(
                    time = OnboardingQuestions.formatTime(reminders?.hour ?: 18, reminders?.minute ?: 0),
                    onClick = onTimeClick,
                )
            }
        }
    }
}

private enum class FooterMode {
    Welcome,
    Next,
    Editing,
    Reminders,
    Build,
}

// The footer for every onboarding step. Single-button steps (the welcome, Build my week) use the
// lower slot, so a swap is a crossfade in place that never moves the thumb target.
@Composable
internal fun OnboardingFooter(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
    // Let's go fades in after the welcome's content, the first time it shows
    animateIn: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    val mode = when {
        state.step == OnboardingViewModel.Step.Welcome -> FooterMode.Welcome
        state.activeQuestion is OnboardingQuestion.Reminders -> FooterMode.Reminders
        state.activeQuestionId == null -> FooterMode.Build
        state.isEditing -> FooterMode.Editing
        else -> FooterMode.Next
    }
    // AppFooter crossfades the actions in place when they change
    val actions = when (mode) {
        FooterMode.Welcome -> listOf(
            AppFooterAction(
                text = stringResource(R.string.onboarding_lets_go),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onStart()
                },
            ),
        )
        FooterMode.Next -> listOf(
            AppFooterAction(text = stringResource(R.string.onboarding_next), onClick = viewModel::onSubmit, enabled = state.canContinue),
            AppFooterAction(text = stringResource(R.string.onboarding_skip), onClick = viewModel::onSkip, style = AppFooterAction.Style.Secondary),
        )
        FooterMode.Editing -> listOf(
            AppFooterAction(text = stringResource(R.string.done), onClick = viewModel::onSubmit, enabled = state.canContinue),
            AppFooterAction(text = stringResource(R.string.cancel), onClick = viewModel::onCancel, style = AppFooterAction.Style.Tertiary),
        )
        FooterMode.Reminders -> listOf(
            AppFooterAction(text = stringResource(R.string.onboarding_remind_me), onClick = viewModel::onSubmit, enabled = state.canContinue),
            AppFooterAction(text = stringResource(R.string.onboarding_not_now), onClick = viewModel::onNotNow, style = AppFooterAction.Style.Secondary),
        )
        FooterMode.Build -> listOf(
            AppFooterAction(text = stringResource(R.string.onboarding_build_my_week), onClick = viewModel::onBuild),
        )
    }
    // Let's go fades in after the welcome's content, the first time it shows
    EnterAnimation(
        animateIn = animateIn && mode == FooterMode.Welcome,
        delayMillis = WelcomeMotion.ButtonDelay,
        slide = false,
        durationMillis = WelcomeMotion.ButtonFade,
        easing = OnboardingMotion.Standard,
    ) {
        AppFooter(actions = actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AppDialog(
        title = stringResource(R.string.onboarding_reminder_time_title),
        onDismissRequest = onDismiss,
        primaryAction = AppDialogAction(
            text = stringResource(R.string.ok),
            onClick = { onConfirm(pickerState.hour, pickerState.minute) },
        ),
        secondaryAction = AppDialogAction(text = stringResource(R.string.cancel), onClick = onDismiss),
    ) {
        TimePicker(
            state = pickerState,
            colors = TimePickerDefaults.colors(
                clockDialColor = Theme.backgrounds.surfaceRaised,
                clockDialSelectedContentColor = Theme.contentColors.onAction,
                clockDialUnselectedContentColor = Theme.contentColors.primary,
                selectorColor = Theme.contentColors.action,
                containerColor = Theme.backgrounds.surface,
                timeSelectorSelectedContainerColor = Theme.backgrounds.actionMuted,
                timeSelectorUnselectedContainerColor = Theme.backgrounds.surfaceRaised,
                timeSelectorSelectedContentColor = Theme.contentColors.primary,
                timeSelectorUnselectedContentColor = Theme.contentColors.primary,
            ),
        )
    }
}

// How an answer reads on its question's summary row
@Composable
private fun answerSummary(question: OnboardingQuestion, answer: Answer, firstDayOfWeek: DayOfWeek): String = when (answer) {
    is Answer.Choice -> (question as? OnboardingQuestion.Choice)?.options
        ?.firstOrNull { it.id == answer.optionId }
        ?.let { stringResource(it.summary) }
        .orEmpty()
    is Answer.Days -> (question as? OnboardingQuestion.Days)
        ?.let { pluralStringResource(it.answer, answer.count, answer.count) }
        .orEmpty()
    is Answer.Reminders -> stringResource(
        R.string.onboarding_reminders_answer,
        OnboardingQuestions.week(startingOn = firstDayOfWeek).filter { it in answer.days }.joinToString(", ") { it.shortName() },
        OnboardingQuestions.formatTime(answer.hour, answer.minute),
    )
    Answer.RemindersOff -> stringResource(R.string.onboarding_reminders_off)
}
