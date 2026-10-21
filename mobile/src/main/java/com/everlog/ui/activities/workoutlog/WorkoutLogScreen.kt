package com.everlog.ui.activities.workoutlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutExercise
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSnackbarHost
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.keypad.AppKeypad
import com.everlog.ui.design.elements.keypad.AppKeypadAction
import com.everlog.ui.design.elements.keypad.AppKeypadEntry
import com.everlog.ui.design.elements.keypad.AppKeypadInput
import com.everlog.ui.design.elements.keypad.AppKeypadNote
import com.everlog.ui.design.elements.keypad.AppKeypadTimer
import com.everlog.ui.design.elements.keypad.AppKeypadTimerStyle
import com.everlog.ui.design.elements.keypad.AppKeypadToggle
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.UnitUtils
import kotlinx.coroutines.android.awaitFrame

/**
 * Renders the view-model's state and forwards what's tapped and typed to it.
 */
@Composable
internal fun WorkoutLogScreen(
    viewModel: WorkoutLogViewModel,
    onAddExercise: () -> Unit,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
    onExit: (WorkoutLogExit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    if (state.keepScreenOn) KeepScreenOn()

    LaunchedEffect(state.exit) {
        state.exit?.let {
            awaitFrame()
            onExit(it)
        }
    }

    BackHandler(onBack = viewModel::onBack)

    val input = remember(viewModel) {
        object : AppKeypadInput {
            override fun onDigit(digit: Char) = viewModel.onDigit(digit)
            override fun onDecimal() = viewModel.onDecimal()
            override fun onBackspace() = viewModel.onBackspace()
            override fun onStep(direction: Int) = viewModel.onStep(direction)
        }
    }
    val tableActions = remember(viewModel, onOpenExercise) {
        object : WorkoutTableActions {
            override fun onCellTapped(cell: CellFocus) = viewModel.onCellTapped(cell)
            override fun onTick(ref: SetRef) = viewModel.onTick(ref)
            override fun onStartTimer(ref: SetRef) = viewModel.onStartSetTimer(ref)
            override fun onAddSet(blockId: Long) = viewModel.onAddSet(blockId)
            override fun onDeleteSet(ref: SetRef) = viewModel.onDeleteSet(ref)
            override fun onOpenExercise(exercise: ELExercise, tab: ExerciseTab) = onOpenExercise(exercise, tab)
            override fun onDeleteExercise(blockId: Long) = viewModel.onDeleteExercise(blockId)
        }
    }
    val addExercise = AppKeypadAction(stringResource(R.string.workout_log_add_exercise), onClick = onAddExercise)
    val empty = state.blocks.isEmpty()

    AppScreen(
        topBar = { Header(state, viewModel) },
        footer = {
            if (state.editing) {
                AppFooter(
                    modifier = Modifier.testTag(WorkoutLogTags.EditFooter),
                    header = stringResource(R.string.workout_log_edit_hint),
                    actions = listOf(
                        AppFooterAction(text = stringResource(R.string.workout_log_done), onClick = viewModel::onEditDone),
                        AppFooterAction(text = stringResource(R.string.workout_log_add_exercise), onClick = onAddExercise, style = AppFooterAction.Style.Secondary),
                    ),
                )
            } else {
                AppKeypad(
                    modifier = Modifier
                        .testTag(WorkoutLogTags.Keypad)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small),
                    entry = keypadEntry(state, viewModel),
                    input = input,
                    onHide = viewModel::onHideKeypad,
                    onShow = if (empty) null else viewModel::onShowKeypad,
                    actions = if (empty) emptyList() else listOf(addExercise),
                    primaryAction = when {
                        empty -> addExercise
                        state.target != null -> AppKeypadAction(
                            text = stringResource(if (state.targetStartsTimer) R.string.workout_log_start_timer else R.string.workout_log_complete_set),
                            icon = ImageVector.vectorResource(if (state.targetStartsTimer) R.drawable.ic_timer else R.drawable.ic_check),
                            onClick = viewModel::onCompleteTarget,
                        )
                        // With every set done, the header's Finish is the main action instead
                        else -> null
                    },
                    strips = {
                        if (empty) AppKeypadNote(text = stringResource(R.string.workout_log_empty_hint))
                        val focus = state.focus
                        if (focus != null && state.blocks.set(focus.ref)?.isSuggested(focus.field) == true) {
                            AppKeypadNote(text = stringResource(R.string.workout_log_suggested, SuggestedMark), highlighted = true)
                        }
                        state.timer?.let { TimerStrip(it, state.timerRemainingSeconds(), viewModel) }
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.editing) {
                EditList(
                    blocks = state.blocks,
                    contentPadding = contentPadding,
                    onMove = viewModel::onMoveExercise,
                    onDelete = viewModel::onDeleteExercise,
                )
            } else if (empty) {
                EmptyTable(contentPadding, state.unit)
            } else {
                WorkoutTable(
                    blocks = state.blocks,
                    view = state.tableView(unitLabel(state.unit)),
                    reveal = state.reveal,
                    contentPadding = contentPadding,
                    actions = tableActions,
                )
            }
            Messages(state, viewModel, Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding()))
        }
    }

    WorkoutLogDialogs(state, viewModel)
}

@Composable
private fun Messages(state: WorkoutLogState, viewModel: WorkoutLogViewModel, modifier: Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(R.string.workout_log_set_deleted)
    val undo = stringResource(R.string.workout_log_undo)
    LaunchedEffect(state.deletion) {
        if (state.deletion == null) {
            // Another change ended the undo
            snackbar.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val result = snackbar.showSnackbar(message = deleted, actionLabel = undo, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete() else viewModel.onUndoGone()
    }
    val message = when (val it = state.message) {
        is WorkoutLogMessage.TooManySets -> stringResource(R.string.exercises_max_sets, it.max)
        null -> null
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onMessageShown()
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(message)
    }
    AppSnackbarHost(hostState = snackbar, modifier = modifier)
}

@Composable
private fun TimerStrip(timer: WorkoutTimer, remainingSeconds: Int, viewModel: WorkoutLogViewModel) {
    val rest = timer is WorkoutTimer.Rest
    AppKeypadTimer(
        label = stringResource(if (rest) R.string.workout_log_rest else R.string.workout_log_set_timer),
        time = formatClock(remainingSeconds.toLong()),
        style = if (rest) AppKeypadTimerStyle.Rest else AppKeypadTimerStyle.Active,
        less = AppKeypadAction(stringResource(R.string.workout_log_timer_less)) { viewModel.onTimerStep(-1) },
        more = AppKeypadAction(stringResource(R.string.workout_log_timer_more)) { viewModel.onTimerStep(1) },
        end = AppKeypadAction(stringResource(if (rest) R.string.workout_log_skip else R.string.workout_log_stop), onClick = viewModel::onTimerEnd),
    )
}

@Composable
private fun keypadEntry(state: WorkoutLogState, viewModel: WorkoutLogViewModel): AppKeypadEntry? {
    val focus = state.focus ?: return null
    val set = state.blocks.set(focus.ref) ?: return null
    val mainKey = state.mainKey ?: return null
    return AppKeypadEntry(
        label = focusLabel(state.blocks, focus, state.unit),
        title = exerciseName(state.blocks, focus.ref),
        decimal = focus.field == ValueField.Weight,
        toggle = AppKeypadToggle(
            options = listOf(stringResource(R.string.workout_log_reps), stringResource(R.string.workout_log_time)),
            selected = if (set.mode == SetMode.Time) 1 else 0,
            onSelect = { viewModel.onModeSelected(if (it == 1) SetMode.Time else SetMode.Reps) },
        ),
        mainKey = AppKeypadAction(
            text = stringResource(
                when (mainKey) {
                    KeypadMainKey.Next -> R.string.workout_log_next
                    KeypadMainKey.CompleteSet -> R.string.workout_log_complete_set
                    KeypadMainKey.StartTimer -> R.string.workout_log_start_timer
                    KeypadMainKey.Done -> R.string.workout_log_done
                },
            ),
            // The tick's and the timer button's icons, for the keys that press them
            icon = when (mainKey) {
                KeypadMainKey.CompleteSet -> ImageVector.vectorResource(R.drawable.ic_check)
                KeypadMainKey.StartTimer -> ImageVector.vectorResource(R.drawable.ic_timer)
                KeypadMainKey.Next, KeypadMainKey.Done -> null
            },
            onClick = viewModel::onMainKey,
        ),
    )
}

@Composable
private fun Header(state: WorkoutLogState, viewModel: WorkoutLogViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIconButton(onClick = viewModel::onClose, icon = ImageVector.vectorResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.workout_log_close))
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = formatClock(state.elapsedSeconds), style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"), maxLines = 1)
            if (state.name.isNotEmpty()) {
                AppText(text = state.name, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
            }
        }
        // Editing ends with the footer's Done
        if (!state.editing) WorkoutOptionsButton(state, viewModel)
        when {
            state.blocks.isEmpty() || state.editing -> Unit
            state.unfinishedSets == 0 -> AppButton(onClick = viewModel::onFinish, text = stringResource(R.string.workout_finish))
            else -> AppSecondaryButton(onClick = viewModel::onFinish, text = stringResource(R.string.workout_finish))
        }
    }
}

// A faded example of what the table becomes, which can't be touched
@Composable
private fun EmptyTable(contentPadding: PaddingValues, unit: SettingsManager.WeightUnit) {
    val name = stringResource(R.string.workout_log_example_exercise)
    val example = remember(name, unit) {
        // A round number in either unit
        val weightKg = if (unit == SettingsManager.WeightUnit.POUND) UnitUtils.lbToKg(45f) else 20f
        val set = WorkoutSet(reps = 12, weightKg = weightKg)
        listOf(
            WorkoutBlock(
                id = -1,
                uuid = null,
                type = null,
                restSeconds = 0,
                exercises = listOf(WorkoutExercise(null, ELExercise(name = name))),
                rounds = listOf(
                    WorkoutRound(-2, listOf(set.copy(completedDate = 1))),
                    WorkoutRound(-3, listOf(set)),
                    WorkoutRound(-4, listOf(set)),
                ),
            ),
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AppText(
            modifier = Modifier
                .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small)
                .clip(Theme.shapes.button)
                .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
                .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
            text = stringResource(R.string.workout_log_example),
            style = Theme.typography.small,
            color = Theme.contentColors.secondary,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            // Faded as one layer, so overlapping parts don't show through each other
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.35f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .clearAndSetSemantics {},
            ) {
                WorkoutTable(
                    blocks = example,
                    view = TableView(unit = unit, unitLabel = unitLabel(unit), selected = SetRef(-1, -3, 0)),
                    reveal = 0,
                    contentPadding = contentPadding,
                    actions = object : WorkoutTableActions {},
                )
            }
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            })
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

// SET 2 OF 4 · KG, with a superset's exercise: SET 1 OF 3 · 2 OF 2 · REPS
@Composable
private fun focusLabel(blocks: List<WorkoutBlock>, focus: CellFocus, unit: SettingsManager.WeightUnit): String {
    val block = blocks.block(focus.ref.blockId) ?: return ""
    val round = block.rounds.indexOfFirst { it.id == focus.ref.roundId } + 1
    val field = when (focus.field) {
        ValueField.Weight -> unitLabel(unit)
        ValueField.Reps -> stringResource(R.string.workout_log_column_reps)
        ValueField.Time -> stringResource(R.string.workout_log_column_time)
    }
    return if (block.exercises.size > 1) {
        stringResource(R.string.workout_log_keypad_label_group, round, block.rounds.size, focus.ref.index + 1, block.exercises.size, field)
    } else {
        stringResource(R.string.workout_log_keypad_label, round, block.rounds.size, field)
    }
}

private fun exerciseName(blocks: List<WorkoutBlock>, ref: SetRef): String =
    blocks.block(ref.blockId)?.exercises?.getOrNull(ref.index)?.exercise?.name.orEmpty()
