package com.everlog.android.workout.feature.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppFooter
import com.everlog.android.common.compose.elements.AppFooterAction
import com.everlog.android.common.compose.elements.AppIconButton
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.keypad.AppKeypad
import com.everlog.android.common.compose.elements.keypad.AppKeypadAction
import com.everlog.android.common.compose.elements.keypad.AppKeypadEntry
import com.everlog.android.common.compose.elements.keypad.AppKeypadInput
import com.everlog.android.common.compose.elements.keypad.AppKeypadNote
import com.everlog.android.common.compose.elements.keypad.AppKeypadTimer
import com.everlog.android.common.compose.elements.keypad.AppKeypadTimerStyle
import com.everlog.android.common.compose.elements.keypad.AppKeypadToggle
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.UnitUtils
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.ValueField
import com.everlog.android.common.session.WorkoutBlock
import com.everlog.android.common.session.WorkoutExercise
import com.everlog.android.common.session.WorkoutRound
import com.everlog.android.common.session.WorkoutSet
import com.everlog.android.common.session.set
import com.everlog.android.workout.feature.setsuggestions.SetSuggestion
import com.everlog.android.common.sessionui.exercisetable.CellFocus
import com.everlog.android.common.sessionui.exercisetable.ExampleTable
import com.everlog.android.common.sessionui.exercisetable.ExerciseEditList
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.common.sessionui.exercisetable.ExerciseTable
import com.everlog.android.common.sessionui.exercisetable.ExerciseTableActions
import com.everlog.android.common.sessionui.exercisetable.SuggestedMark
import com.everlog.android.common.sessionui.exercisetable.TableView
import com.everlog.android.common.sessionui.exercisetable.exerciseName
import com.everlog.android.common.sessionui.exercisetable.focusLabel
import com.everlog.android.workout.feature.R
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.kotlin.format.displayWeight
import com.everlog.android.common.kotlin.format.formatWeight
import com.everlog.android.common.kotlin.format.unitLabel
import com.everlog.android.common.kotlin.format.unitName
import kotlinx.coroutines.android.awaitFrame
import com.everlog.android.common.design.R as DesignR
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Renders the view-model's state and forwards what's tapped and typed to it.
 */
@Composable
internal fun WorkoutLogScreen(
    viewModel: WorkoutLogViewModel,
    onAddExercise: () -> Unit,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
    onOpenSetSuggestions: () -> Unit,
    onExit: (WorkoutLogExit) -> Unit,
    // Null when there's no feedback form, which leaves it out of the options
    onSendFeedback: (() -> Unit)? = null,
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
        object : ExerciseTableActions {
            override fun onCellTapped(cell: CellFocus) = viewModel.onCellTapped(cell)
            override fun onTick(ref: SetRef) = viewModel.onTick(ref)
            override fun onStartTimer(ref: SetRef) = viewModel.onStartSetTimer(ref)
            override fun onAddSet(blockId: Long) = viewModel.onAddSet(blockId)
            override fun onDeleteSet(ref: SetRef) = viewModel.onDeleteSet(ref)
            override fun onOpenExercise(exercise: ELExercise, tab: ExerciseTab) = onOpenExercise(exercise, tab)
            override fun onDeleteExercise(blockId: Long) = viewModel.onDeleteExercise(blockId)
        }
    }
    val openSetSuggestions = {
        viewModel.onSetSuggestionsTapped()
        onOpenSetSuggestions()
    }
    val addExercise = AppKeypadAction(stringResource(DesignR.string.workout_log_add_exercise), onClick = onAddExercise)
    val empty = state.blocks.isEmpty()

    AppScreen(
        topBar = { Header(state, viewModel, openSetSuggestions, onSendFeedback) },
        footer = {
            if (state.editing) {
                AppFooter(
                    modifier = Modifier.testTag(WorkoutLogTags.EditFooter),
                    header = stringResource(R.string.workout_log_edit_hint),
                    actions = listOf(
                        AppFooterAction(text = stringResource(DesignR.string.workout_log_done), onClick = viewModel::onEditDone),
                        AppFooterAction(text = stringResource(DesignR.string.workout_log_add_exercise), onClick = onAddExercise, style = AppFooterAction.Style.Secondary),
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
                            text = stringResource(if (state.targetStartsTimer) DesignR.string.workout_log_start_timer else DesignR.string.workout_log_complete_set),
                            icon = ImageVector.vectorResource(if (state.targetStartsTimer) DesignR.drawable.ic_timer else DesignR.drawable.ic_check),
                            onClick = viewModel::onCompleteTarget,
                        )
                        // With every set done, the header's Finish is the main action instead
                        else -> null
                    },
                    strips = {
                        if (empty) AppKeypadNote(text = stringResource(R.string.workout_log_empty_hint))
                        state.suggestionNote?.let { note ->
                            AppKeypadNote(
                                modifier = Modifier.testTag(WorkoutLogTags.SuggestionNote),
                                text = suggestionNoteText(note, state.unit),
                                highlighted = true,
                                action = AppKeypadAction(stringResource(R.string.workout_log_suggestions_change), onClick = openSetSuggestions),
                            )
                        }
                        state.timer?.let { TimerStrip(it, state.timerRemainingSeconds(), viewModel) }
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.editing) {
                ExerciseEditList(
                    blocks = state.blocks,
                    contentPadding = contentPadding,
                    onMove = viewModel::onMoveExercise,
                    onDelete = viewModel::onDeleteExercise,
                )
            } else if (empty) {
                EmptyTable(contentPadding, state.unit)
            } else {
                ExerciseTable(
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
    val deleted = stringResource(DesignR.string.workout_log_set_deleted)
    val undo = stringResource(DesignR.string.workout_log_undo)
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
        is WorkoutLogMessage.TooManySets -> stringResource(DesignR.string.exercises_max_sets, it.max)
        null -> null
    }
    // Shown outside the effect: clearing the message restarts the effect, which would cancel the snackbar with it
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onMessageShown()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
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
            options = listOf(stringResource(DesignR.string.workout_log_reps), stringResource(DesignR.string.workout_log_time)),
            selected = if (set.mode == SetMode.Time) 1 else 0,
            onSelect = { viewModel.onModeSelected(if (it == 1) SetMode.Time else SetMode.Reps) },
        ),
        mainKey = AppKeypadAction(
            text = stringResource(
                when (mainKey) {
                    KeypadMainKey.Next -> R.string.workout_log_next
                    KeypadMainKey.CompleteSet -> DesignR.string.workout_log_complete_set
                    KeypadMainKey.StartTimer -> DesignR.string.workout_log_start_timer
                    KeypadMainKey.Done -> DesignR.string.workout_log_done
                },
            ),
            // The tick's and the timer button's icons, for the keys that press them
            icon = when (mainKey) {
                KeypadMainKey.CompleteSet -> ImageVector.vectorResource(DesignR.drawable.ic_check)
                KeypadMainKey.StartTimer -> ImageVector.vectorResource(DesignR.drawable.ic_timer)
                KeypadMainKey.Next, KeypadMainKey.Done -> null
            },
            onClick = viewModel::onMainKey,
        ),
    )
}

@Composable
private fun Header(state: WorkoutLogState, viewModel: WorkoutLogViewModel, onOpenSetSuggestions: () -> Unit, onSendFeedback: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIconButton(onClick = viewModel::onClose, icon = ImageVector.vectorResource(DesignR.drawable.ic_remove), contentDescription = stringResource(R.string.workout_log_close))
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = formatClock(state.elapsedSeconds), style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"), maxLines = 1)
            if (state.name.isNotEmpty()) {
                AppText(text = state.name, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
            }
        }
        // Editing ends with the footer's Done
        if (!state.editing) WorkoutOptionsButton(state, viewModel, onOpenSetSuggestions, onSendFeedback)
        when {
            state.blocks.isEmpty() || state.editing -> Unit
            state.unfinishedSets == 0 -> AppButton(onClick = viewModel::onFinish, text = stringResource(DesignR.string.workout_finish))
            else -> AppSecondaryButton(onClick = viewModel::onFinish, text = stringResource(DesignR.string.workout_finish))
        }
    }
}

// A faded example of what the table becomes, which can't be touched
@Composable
private fun EmptyTable(contentPadding: PaddingValues, unit: WeightUnit) {
    val name = stringResource(DesignR.string.workout_log_example_exercise)
    val example = remember(name, unit) {
        // A round number in either unit
        val weightKg = if (unit == WeightUnit.POUND) UnitUtils.lbToKg(45f) else 20f
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
    ExampleTable(
        blocks = example,
        view = TableView(unit = unit, unitLabel = unitLabel(unit), selected = SetRef(-1, -3, 0)),
        contentPadding = contentPadding,
    )
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun suggestionNoteText(note: SuggestionNote, unit: WeightUnit): String = when (note) {
    SuggestionNote.LastTime -> stringResource(R.string.workout_log_suggested, SuggestedMark)
    is SuggestionNote.Goal -> stringResource(
        R.string.workout_log_suggested_goal,
        SuggestedMark,
        formatWeight(displayWeight(note.weightKg, unit)),
        unitName(unit),
        SetSuggestion.of(note.goal)?.let { stringResource(it.title) } ?: note.goal.valueName(LocalContext.current),
        (note.goal.percent1RM() * 100).roundToInt(),
    )
}
