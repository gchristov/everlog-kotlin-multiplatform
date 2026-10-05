package com.everlog.ui.activities.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import org.threeten.bp.DayOfWeek

// Debug-only prototype of the onboarding (first run) journey from the Everlog Onboarding design,
// opened from Settings. UI only: answers aren't saved, nothing is logged, and Build my routine
// closes the screen. The building, reveal and end steps come later.
class OnboardingActivity : CommonComposeActivity() {
    private val viewModel by viewModels<OnboardingViewModel> {
        createViewModelFactory {
            OnboardingViewModel(
                dispatcher = Dispatchers.Main,
                firstDayOfWeek = SettingsManager.manager.firstDayOfWeek(),
            )
        }
    }

    @Composable
    override fun Content() = OnboardingScreen(
        viewModel = viewModel,
        onClose = { finish() },
    )
}

@Composable
internal fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSkipSetup by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = !state.finished) {
        showSkipSetup = true
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    Setup(
        state = state,
        viewModel = viewModel,
        onSkipSetup = { showSkipSetup = true },
    )

    var skippingSetup by remember { mutableStateOf(false) }
    if (showSkipSetup) {
        SkipSetupDialog(
            onKeepGoing = { showSkipSetup = false },
            onSkipSetup = {
                showSkipSetup = false
                skippingSetup = true
            },
        )
    }
    // Close only once the dialog has gone. Its dim covers the whole task, so closing with it open
    // leaves the screen underneath dimmed until the close animation ends.
    if (skippingSetup) {
        LaunchedEffect(Unit) {
            awaitFrame()
            onClose()
        }
    }
}

@Composable
private fun Setup(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
    onSkipSetup: () -> Unit,
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

    AppScreen(
        topBar = {
            OnboardingTopBar(
                step = state.step,
                total = state.questions.size,
                onSkipSetup = onSkipSetup,
            )
        },
        footer = { SetupFooter(state = state, viewModel = viewModel) },
    ) { contentPadding ->
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
    // The first question sits lower on an empty screen, like the design's first state
    val topPadding by animateDpAsState(
        targetValue = if (state.answers.isEmpty()) 72.dp else 16.dp,
        animationSpec = tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard),
        label = "topPadding",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = topPadding),
    ) {
        state.questions.forEach { question ->
            AnimatedVisibility(
                modifier = Modifier.onGloballyPositioned { onQuestionPositioned(question.id, it.positionInRoot().y) },
                visible = question.id in visibleIds,
                // A new question fades in and slides up as the previous one collapses
                enter = fadeIn(tween(OnboardingMotion.Enter, OnboardingMotion.AppendDelay, OnboardingMotion.EmphasizedDecelerate)) +
                        slideInVertically(tween(OnboardingMotion.Enter, OnboardingMotion.AppendDelay, OnboardingMotion.EmphasizedDecelerate)) { enterOffset },
                exit = fadeOut(tween(150)),
            ) {
                QuestionItem(
                    question = question,
                    state = state,
                    viewModel = viewModel,
                    onTimeClick = onTimeClick,
                )
            }
        }
        // Room for the last questions to reach the upper third, above the footer
        VerticalSpace(height = with(density) { (viewportHeight / 2f).toDp() } + contentPadding.calculateBottomPadding())
    }
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
                fadeIn(tween(200, delayMillis = 50, easing = OnboardingMotion.EmphasizedDecelerate))
            } else {
                // Collapsing: the summary row fades in once the options have gone
                fadeIn(tween(150, delayMillis = 100, easing = OnboardingMotion.Standard))
            }
            (enter togetherWith fadeOut(tween(120, easing = OnboardingMotion.EmphasizedAccelerate)))
                .using(SizeTransform(clip = true) { _, _ -> tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard) })
        },
        label = "question",
    ) { item ->
        when (item.mode) {
            ItemMode.Open -> Box(modifier = Modifier.padding(top = if (state.answers.isEmpty()) 0.dp else 16.dp, bottom = 24.dp)) {
                QuestionBlock(
                    question = question,
                    input = item.input,
                    editing = item.editing,
                    viewModel = viewModel,
                    onTimeClick = onTimeClick,
                )
            }
            ItemMode.Answered -> Box(modifier = Modifier.padding(bottom = 8.dp)) {
                SummaryRow(
                    label = question.label,
                    answer = state.answers[question.id]?.let { OnboardingQuestions.summary(question, it, state.firstDayOfWeek) }.orEmpty(),
                    skipped = question.id in state.skipped,
                    onClick = { viewModel.onEdit(question.id) },
                )
            }
            ItemMode.UpNext -> Box(modifier = Modifier.padding(bottom = 8.dp)) {
                UpNextRow(label = question.label)
            }
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
                text = "Editing",
                style = OnboardingType.Helper.copy(fontWeight = FontWeight.Medium),
                color = Theme.contentColors.action,
            )
            VerticalSpace(6.dp)
        }
        AppText(text = question.title, style = OnboardingType.QuestionTitle)
        question.helper?.let { helper ->
            VerticalSpace(8.dp)
            AppText(text = helper, style = OnboardingType.Helper, color = Theme.contentColors.secondary)
        }
        VerticalSpace(if (question is OnboardingQuestion.Choice && question.sideBySide) 24.dp else 20.dp)
        when (question) {
            is OnboardingQuestion.Choice -> ChoiceOptions(
                question = question,
                selectedId = (input as? Answer.Choice)?.optionId,
                onSelect = { viewModel.onOptionSelect(question.id, it) },
            )
            // Same squares and spacing as the reminder days
            is OnboardingQuestion.Days -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                question.options.forEach { count ->
                    NumberChip(
                        modifier = Modifier.weight(1f),
                        value = count,
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEach { option ->
                UnitCard(
                    modifier = Modifier.weight(1f),
                    title = option.title,
                    description = option.description.orEmpty(),
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.forEach { option ->
                SelectableListItem(
                    title = option.title,
                    description = option.description,
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        VerticalSpace(16.dp)
        TimeRow(
            time = OnboardingQuestions.formatTime(reminders?.hour ?: 18, reminders?.minute ?: 0),
            onClick = onTimeClick,
        )
    }
}

private enum class FooterMode {
    Next,
    Editing,
    Reminders,
    Build,
}

@Composable
private fun SetupFooter(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
) {
    val mode = when {
        state.activeQuestion is OnboardingQuestion.Reminders -> FooterMode.Reminders
        state.activeQuestionId == null -> FooterMode.Build
        state.isEditing -> FooterMode.Editing
        else -> FooterMode.Next
    }
    // A footer swap is a crossfade with no movement
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            fadeIn(tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard)) togetherWith
                    fadeOut(tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard))
        },
        label = "footer",
    ) { footerMode ->
        AppFooter(
            actions = when (footerMode) {
                FooterMode.Next -> listOf(
                    AppFooterAction(text = "Next", onClick = viewModel::onSubmit, enabled = state.canContinue),
                    AppFooterAction(text = "Skip", onClick = viewModel::onSkip, style = AppFooterAction.Style.Secondary),
                )
                FooterMode.Editing -> listOf(
                    AppFooterAction(text = "Done", onClick = viewModel::onSubmit, enabled = state.canContinue),
                    AppFooterAction(text = "Cancel", onClick = viewModel::onCancel, style = AppFooterAction.Style.Tertiary),
                )
                FooterMode.Reminders -> listOf(
                    AppFooterAction(text = "Remind me", onClick = viewModel::onSubmit, enabled = state.canContinue),
                    AppFooterAction(text = "Not now", onClick = viewModel::onNotNow, style = AppFooterAction.Style.Secondary),
                )
                FooterMode.Build -> listOf(
                    AppFooterAction(text = "Build my routine", onClick = viewModel::onBuild),
                )
            }
        )
    }
}

@Composable
private fun SkipSetupDialog(
    onKeepGoing: () -> Unit,
    onSkipSetup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepGoing,
        containerColor = Theme.backgrounds.surface,
        title = { AppText(text = "Skip setup?", style = Theme.typography.heading) },
        text = {
            AppText(
                text = "We'll start you with an empty app. You can build routines yourself, or set this up later from Settings.",
                color = Theme.contentColors.secondary,
            )
        },
        // The action we want them to take is primary, leaving is secondary
        confirmButton = {
            AppButton(onClick = onKeepGoing, text = "Keep going")
        },
        dismissButton = {
            AppSecondaryButton(onClick = onSkipSetup, text = "Skip setup")
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val colors = onboardingColors()
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.backgrounds.surface,
        text = {
            TimePicker(
                state = pickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Theme.backgrounds.surfaceRaised,
                    clockDialSelectedContentColor = Theme.contentColors.onAction,
                    clockDialUnselectedContentColor = Theme.contentColors.primary,
                    selectorColor = Theme.contentColors.action,
                    containerColor = Theme.backgrounds.surface,
                    timeSelectorSelectedContainerColor = colors.accentFillCurrent,
                    timeSelectorUnselectedContainerColor = Theme.backgrounds.surfaceRaised,
                    timeSelectorSelectedContentColor = Theme.contentColors.primary,
                    timeSelectorUnselectedContentColor = Theme.contentColors.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                AppText(text = "OK", style = OnboardingType.TextAction, color = Theme.contentColors.action)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AppText(text = "Cancel", style = OnboardingType.TextAction, color = Theme.contentColors.secondary)
            }
        },
    )
}
