package com.everlog.ui.activities.workoutlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
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
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.keypad.AppKeypad
import com.everlog.ui.design.elements.keypad.AppKeypadAction
import com.everlog.ui.design.elements.keypad.AppKeypadEntry
import com.everlog.ui.design.elements.keypad.AppKeypadInput
import com.everlog.ui.design.elements.keypad.AppKeypadNote
import com.everlog.ui.design.elements.keypad.AppKeypadToggle
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.navigator.ELNavigator
import com.everlog.utils.UnitUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

/**
 * The workout screen in Compose (TAS-455), for workouts started while Remote Config's workout_screen_v2 is on (see
 * ELNavigator.workoutActivity). Opened with the workout as [ELConstants.EXTRA_WORKOUT], like WorkoutActivity.
 */
class WorkoutLogActivity : CommonComposeActivity() {
    private val viewModel by viewModels<WorkoutLogViewModel> {
        viewModelFactory {
            initializer {
                val store = RealWorkoutLogStore()
                @Suppress("DEPRECATION")
                val launched = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as ELWorkout
                WorkoutLogViewModel(
                    dispatcher = Dispatchers.Main,
                    workout = WorkoutLogViewModel.load(launched, store),
                    fromPlan = intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false),
                    store = store,
                    analytics = RealWorkoutLogAnalytics(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // Closes screens that shouldn't stay under a workout, as the old screen does
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        }
    }

    @Composable
    override fun Content() = WorkoutLogScreen(
        viewModel = viewModel,
        onAddExercise = { ELNavigator(this).openExercisePicker() },
        onExit = ::exit,
    )

    @Deprecated("The navigator opens the exercise picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES && resultCode == RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>)?.let(viewModel::onExercisesPicked)
        }
    }

    private fun exit(exit: WorkoutLogExit) {
        if (exit is WorkoutLogExit.Finished) {
            ELNavigator(this).openWorkoutDetails(exit.workout, true, intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false))
        }
        finish()
    }
}

@Composable
internal fun WorkoutLogScreen(viewModel: WorkoutLogViewModel, onAddExercise: () -> Unit, onExit: (WorkoutLogExit) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The cell the keypad types into; the keypad is open while there is one
    var focus by remember { mutableStateOf<CellFocus?>(null) }
    // The highlighted set: the current one as the workout starts, then the last one touched. Once it's completed,
    // the next set.
    var selected by remember { mutableStateOf(state.current) }
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    var typed by remember { mutableStateOf<String?>(null) }
    // Goes up with every key, so the edited cell comes back into view if it was scrolled away
    var reveal by remember { mutableIntStateOf(0) }
    var keepScreenOn by remember { mutableStateOf(SettingsManager.manager.keepScreenOn()) }

    // On resume, before any of the screen's events, so they're credited to it. Settings may have changed meanwhile.
    LifecycleResumeEffect(Unit) {
        AnalyticsManager.manager.screenName(AnalyticsConstants.SCREEN_WORKOUT_V2)
        keepScreenOn = SettingsManager.manager.keepScreenOn()
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    if (keepScreenOn) KeepScreenOn()

    LaunchedEffect(state.exit) {
        state.exit?.let {
            awaitFrame()
            onExit(it)
        }
    }

    BackHandler {
        if (focus != null) focus = null else viewModel.onClose()
    }

    // A set that's gone can't keep the keypad's focus or the selection
    LaunchedEffect(state.blocks) {
        focus?.let { if (state.blocks.set(it.ref) == null) focus = null }
        selected?.let { if (state.blocks.set(it) == null) selected = state.current }
    }

    fun focusOn(cell: CellFocus?) {
        focus = cell
        typed = null
        cell?.let { selected = it.ref }
    }

    // The selected set got completed: select the next one, and move the keypad there if it's open
    LaunchedEffect(state.current) {
        val current = selected
        if (current == null || state.blocks.set(current) == null) {
            selected = state.current
            return@LaunchedEffect
        }
        val next = state.current
        if (state.blocks.set(current)?.done == true && next != null) {
            selected = next
            if (focus != null) focusOn(state.blocks.set(next)?.let { CellFocus(next, countField(it)) })
        }
    }

    val step = { field: ValueField ->
        when (field) {
            ValueField.Weight -> viewModel.weightIncrease()
            ValueField.Reps -> RepsStep
            ValueField.Time -> TimeStepSeconds.toFloat()
        }
    }
    val input = object : AppKeypadInput {
        fun type(key: Char) {
            val cell = focus ?: return
            val base = typed ?: ""
            if (key == '.' && (cell.field != ValueField.Weight || base.contains('.'))) return
            val text = (base + key).take(maxLength(cell.field))
            typed = text
            viewModel.onSetValue(cell.ref, cell.field, parse(cell.field, text))
            reveal++
        }

        override fun onDigit(digit: Char) = type(digit)

        override fun onDecimal() = type('.')

        override fun onBackspace() {
            val cell = focus ?: return
            val set = state.blocks.set(cell.ref) ?: return
            val text = (typed ?: rawText(set, cell.field, state.unit)).dropLast(1)
            typed = text
            viewModel.onSetValue(cell.ref, cell.field, parse(cell.field, text))
            reveal++
        }

        override fun onStep(direction: Int) {
            val cell = focus ?: return
            val set = state.blocks.set(cell.ref) ?: return
            val value = when (cell.field) {
                ValueField.Weight -> set.weightKg?.let { displayWeight(it, state.unit) } ?: 0f
                ValueField.Reps -> (set.reps ?: 0).toFloat()
                ValueField.Time -> (set.timeSeconds ?: 0).toFloat()
            }
            typed = null
            viewModel.onSetValue(cell.ref, cell.field, (value + direction * step(cell.field)).coerceAtLeast(0f))
            reveal++
        }
    }

    // Reps (or time), then the weight, whose main key completes the set and moves on to the next set's reps
    val entry = focus?.let { cell ->
        val set = state.blocks.set(cell.ref) ?: return@let null
        AppKeypadEntry(
            label = focusLabel(state.blocks, cell, state.unit),
            title = exerciseName(state.blocks, cell.ref),
            decimal = cell.field == ValueField.Weight,
            toggle = AppKeypadToggle(
                options = listOf(stringResource(R.string.workout_log_reps), stringResource(R.string.workout_log_time)),
                selected = if (set.mode == SetMode.Time) 1 else 0,
                onSelect = { index ->
                    val mode = if (index == 1) SetMode.Time else SetMode.Reps
                    viewModel.onSetMode(cell.ref, mode)
                    if (cell.field != ValueField.Weight) focusOn(CellFocus(cell.ref, if (mode == SetMode.Time) ValueField.Time else ValueField.Reps))
                },
            ),
            mainKey = when {
                cell.field != ValueField.Weight -> AppKeypadAction(stringResource(R.string.workout_log_next)) {
                    focusOn(CellFocus(cell.ref, ValueField.Weight))
                }
                // A done set's values save as they're typed, so its key just finishes editing
                set.done -> AppKeypadAction(stringResource(R.string.workout_log_done)) {
                    focusOn(null)
                    selected = state.current ?: selected
                }
                else -> AppKeypadAction(stringResource(R.string.workout_log_complete_set)) {
                    viewModel.onCompleteSet(cell.ref)
                    val next = viewModel.state.value.current
                    focusOn(next?.let { ref -> viewModel.state.value.blocks.set(ref)?.let { CellFocus(ref, countField(it)) } })
                }
            },
        )
    }
    // The set the closed keypad's main action completes: the selected one, or else the current one
    val target = selected?.takeIf { state.blocks.set(it)?.done == false } ?: state.current
    val addExercise = AppKeypadAction(stringResource(R.string.workout_log_add_exercise), onAddExercise)

    AppScreen(
        topBar = { Header(state, onClose = viewModel::onClose, onFinish = viewModel::onFinish) },
        footer = {
            AppKeypad(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small),
                entry = entry,
                input = input,
                onHide = { focusOn(null) },
                onShow = if (state.blocks.isEmpty()) null else {
                    { (selected ?: state.current)?.let { ref -> state.blocks.set(ref)?.let { focusOn(CellFocus(ref, countField(it))) } } }
                },
                actions = if (state.blocks.isEmpty()) emptyList() else listOf(addExercise),
                primaryAction = when {
                    // The empty workout's only action
                    state.blocks.isEmpty() -> addExercise
                    target != null -> AppKeypadAction(stringResource(R.string.workout_log_complete_set)) { viewModel.onCompleteSet(target) }
                    // Every set done: Finish in the header is filled instead
                    else -> null
                },
                strips = {
                    if (state.blocks.isEmpty()) AppKeypadNote(text = stringResource(R.string.workout_log_empty_hint))
                },
            )
        },
    ) { contentPadding ->
        if (state.blocks.isEmpty()) {
            EmptyTable(contentPadding, state.unit)
        } else {
            WorkoutTable(
                blocks = state.blocks,
                unit = state.unit,
                focus = focus,
                selected = selected,
                typed = typed,
                reveal = reveal,
                contentPadding = contentPadding,
                onCellTapped = { focusOn(it) },
                onTick = { ref ->
                    selected = ref
                    // The keypad follows to the touched set
                    if (focus != null) state.blocks.set(ref)?.let { focusOn(CellFocus(ref, countField(it))) }
                    viewModel.onToggleSet(ref)
                },
            )
        }
    }

    Dialogs(state.dialog, viewModel)
}

// Close, the workout's time with its name under it, and Finish
@Composable
private fun Header(state: WorkoutLogState, onClose: () -> Unit, onFinish: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIconButton(onClick = onClose, icon = ImageVector.vectorResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.workout_log_close))
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = formatClock(state.elapsedSeconds), style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"), maxLines = 1)
            if (state.name.isNotEmpty()) {
                AppText(text = state.name, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
            }
        }
        // Filled once every set is done: it's what's next
        when {
            state.blocks.isEmpty() -> Unit
            state.unfinishedSets == 0 -> AppButton(onClick = onFinish, text = stringResource(R.string.workout_finish))
            else -> AppSecondaryButton(onClick = onFinish, text = stringResource(R.string.workout_finish))
        }
    }
}

// The empty workout: a faded example of what the table becomes, which can't be touched. The keypad has Add exercise.
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
            // Faded as one, so nothing shows through
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
                    unit = unit,
                    focus = null,
                    selected = SetRef(-1, -3, 0),
                    typed = null,
                    reveal = 0,
                    contentPadding = contentPadding,
                    onCellTapped = {},
                    onTick = {},
                )
            }
            // Takes every touch, so the example can't be used
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
private fun Dialogs(dialog: WorkoutLogDialog?, viewModel: WorkoutLogViewModel) {
    when (dialog) {
        null -> Unit
        WorkoutLogDialog.Finish -> AppDialog(
            title = stringResource(R.string.workout_log_finish_title),
            body = stringResource(R.string.workout_log_finish_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.workout_finish), viewModel::onFinishConfirmed),
            secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
        )
        is WorkoutLogDialog.Discard -> if (dialog.setsCompleted > 0) {
            // Finish instead, so logged sets aren't lost by accident; dismissing keeps going
            var body = pluralStringResource(R.plurals.workout_discard_sets_logged, dialog.setsCompleted, dialog.setsCompleted)
            if (dialog.untickedSaved > 0) {
                body += " " + pluralStringResource(R.plurals.workout_discard_unticked_saved, dialog.untickedSaved, dialog.untickedSaved)
            }
            AppDialog(
                title = stringResource(R.string.workout_discard_title),
                body = body,
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(R.string.workout_discard_finish), viewModel::onDiscardFinish),
                secondaryAction = AppDialogAction(stringResource(R.string.discard), viewModel::onDiscardConfirmed),
            )
        } else {
            AppDialog(
                title = stringResource(R.string.workout_discard_title),
                body = stringResource(R.string.workout_discard_nothing_logged),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(R.string.workout_discard_keep_going), viewModel::onDialogDismissed),
                secondaryAction = AppDialogAction(stringResource(R.string.discard), viewModel::onDiscardConfirmed),
            )
        }
    }
}

// The screen stays on through a workout when Settings says so, as on the old screen
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

private const val RepsStep = 1f
private const val TimeStepSeconds = 5

private fun maxLength(field: ValueField) = when (field) {
    ValueField.Weight -> 6
    ValueField.Reps -> 3
    ValueField.Time -> 4
}

// Nothing typed clears the value
private fun parse(field: ValueField, text: String): Float? = when (field) {
    ValueField.Weight -> text.toFloatOrNull()
    ValueField.Reps -> text.toIntOrNull()?.toFloat()
    ValueField.Time -> text.takeIf { it.isNotEmpty() }?.let { typedSeconds(it).toFloat() }
}

// A value as its digits, for backspacing into it
private fun rawText(set: WorkoutSet, field: ValueField, unit: SettingsManager.WeightUnit): String = when (field) {
    ValueField.Weight -> set.weightKg?.let { formatWeight(displayWeight(it, unit)) }.orEmpty()
    ValueField.Reps -> set.reps?.toString().orEmpty()
    ValueField.Time -> set.timeSeconds?.let(::secondsTyped).orEmpty()
}
