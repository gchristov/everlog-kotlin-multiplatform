package com.everlog.ui.activities.workouttable

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import com.everlog.ui.activities.workoutprototype.tagStyle
import com.everlog.ui.activities.workoutprototype.SetValues
import com.everlog.ui.activities.workoutprototype.PrototypeSet
import com.everlog.ui.activities.workoutprototype.PrototypeRound
import com.everlog.ui.activities.workoutprototype.PrototypeExercise
import com.everlog.ui.activities.workoutprototype.PrototypeBlock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.activities.workoutprototype.KeepScreenOn
import com.everlog.ui.activities.workoutprototype.SetMode
import com.everlog.ui.activities.workoutprototype.SetRef
import com.everlog.ui.activities.workoutprototype.ValueField
import com.everlog.ui.activities.workoutprototype.UndoDeletedSet
import com.everlog.ui.activities.workoutprototype.WorkoutDialogs
import com.everlog.ui.activities.workoutprototype.WorkoutSnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeViewModel
import com.everlog.ui.activities.workoutprototype.formatClock
import com.everlog.ui.activities.workoutprototype.formatWeight
import com.everlog.ui.activities.workoutprototype.numberStyle
import com.everlog.ui.activities.workoutprototype.refs
import com.everlog.ui.activities.workoutprototype.set
import com.everlog.ui.activities.workoutprototype.vibrate
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.activities.workoutprototype.EditHint
import com.everlog.ui.activities.workoutprototype.EditList
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

// Debug only, from Settings › Developer: a second workout screen concept to compare with the Spine
// and Dock one. The workout as a table (set, last time, kg, reps, tick), typed into with our own
// keypad docked at the bottom, which stays open once a cell is tapped. Same view-model and mock data
// as the Spine and Dock prototype, so completing, rest, timers and supersets behave the same.
class WorkoutTableActivity : CommonComposeActivity() {
    private val viewModel by viewModels<WorkoutPrototypeViewModel> {
        createViewModelFactory { WorkoutPrototypeViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = WorkoutTableScreen(viewModel = viewModel, onClose = ::finish)
}

// The cell the keypad types into
internal data class CellFocus(val ref: SetRef, val field: ValueField)

@Composable
internal fun WorkoutTableScreen(viewModel: WorkoutPrototypeViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var focus by remember { mutableStateOf<CellFocus?>(null) }
    // The highlighted set: the current one as the workout starts (keypad closed), then the last one
    // touched (row, cell, tick or ▶), and the keypad's while it's open. Once it's completed, the next set.
    var selected by remember { mutableStateOf(viewModel.state.value.current) }
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    var typed by remember { mutableStateOf<String?>(null) }
    // Goes up with every keypad key, so the edited cell comes back into view if it was scrolled away
    var reveal by remember { mutableIntStateOf(0) }

    KeepScreenOn()

    BackHandler {
        when {
            state.editMode -> viewModel.onEditDone()
            focus != null -> focus = null
            else -> viewModel.onDiscard()
        }
    }

    // Editing the workout closes the keypad
    LaunchedEffect(state.editMode) {
        if (state.editMode) focus = null
    }

    LaunchedEffect(state.finished) {
        if (state.finished) {
            awaitFrame()
            onClose()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context.applicationContext, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(state.alerts) {
        if (state.alerts > 0) vibrate(context)
    }

    // A deleted set can't keep the keypad's focus or the selection
    LaunchedEffect(state.blocks) {
        focus?.let { if (state.blocks.set(it.ref) == null) focus = null }
        selected?.let { if (state.blocks.set(it) == null) selected = state.current }
    }

    val snackbar = remember { SnackbarHostState() }
    UndoDeletedSet(state, viewModel, snackbar)

    fun focusOn(cell: CellFocus?) {
        focus = cell
        typed = null
        cell?.let { selected = it.ref }
    }

    // Touching a set's tick or ▶ selects it, and moves the keypad there if it's open
    fun select(ref: SetRef) {
        selected = ref
        if (focus != null) focusOn(CellFocus(ref, countField(state, ref)))
    }

    // The selected set got completed (its tick, ✓ or its timer ending): select the next one
    LaunchedEffect(state.current) {
        val current = selected
        // Nothing selected (e.g. the workout was empty): the current set
        if (current == null || state.blocks.set(current) == null) {
            selected = state.current
            return@LaunchedEffect
        }
        // Done: the next set, if there's one left. Otherwise the last stays selected, so there's always
        // a set the keypad can open on.
        val next = state.current
        if (state.blocks.set(current)?.done == true && next != null) {
            selected = next
            if (focus != null) focusOn(CellFocus(next, countField(state, next)))
        }
    }

    // Every key brings the edited cell back into view
    fun KeypadKeys.revealing() = KeypadKeys(
        // Typing moves on from the effort question
        onDigit = { onDigit(it); reveal++; viewModel.onDismissEffort() },
        onBackspace = { onBackspace(); reveal++; viewModel.onDismissEffort() },
        onStep = { onStep(it); reveal++; viewModel.onDismissEffort() },
        onNext = { onNext(); reveal++ },
        onComplete = { onComplete(); reveal++ },
        onMode = { onMode(it); reveal++ },
        onHide = onHide,
        onDone = onDone,
    )

    val keys = KeypadKeys(
        onDigit = { key ->
            val cell = focus ?: return@KeypadKeys
            val base = typed ?: ""
            if (key == '.' && (cell.field != ValueField.Weight || base.contains('.'))) return@KeypadKeys
            val text = (base + key).take(maxLength(cell.field))
            typed = text
            viewModel.onSetValue(cell.ref, cell.field, parse(cell.field, text))
        },
        onBackspace = {
            val cell = focus ?: return@KeypadKeys
            val values = state.blocks.set(cell.ref)?.values ?: return@KeypadKeys
            val text = (typed ?: rawText(values, cell.field)).dropLast(1)
            typed = text
            viewModel.onSetValue(cell.ref, cell.field, parse(cell.field, text))
        },
        onStep = { direction ->
            val cell = focus ?: return@KeypadKeys
            val values = state.blocks.set(cell.ref)?.values ?: return@KeypadKeys
            val value = when (cell.field) {
                ValueField.Weight -> values.weightKg + direction * WorkoutPrototypeViewModel.WeightStepKg
                ValueField.Reps -> (values.reps + direction * WorkoutPrototypeViewModel.RepsStep).toFloat()
                ValueField.Time -> (values.timeSeconds + direction * WorkoutPrototypeViewModel.TimeStepSeconds).toFloat()
            }
            typed = null
            viewModel.onSetValue(cell.ref, cell.field, value)
        },
        // Reps (or time), then kg, then the next set's reps, in the columns' order
        onNext = {
            val cell = focus ?: return@KeypadKeys
            val timed = state.blocks.set(cell.ref)?.values?.mode == SetMode.Time
            if (cell.field != ValueField.Weight) {
                focusOn(CellFocus(cell.ref, ValueField.Weight))
            } else if (timed) {
                // A timed set ends on its time, where the main key starts it
                focusOn(CellFocus(cell.ref, ValueField.Time))
            } else {
                val refs = state.blocks.refs()
                refs.getOrNull(refs.indexOf(cell.ref) + 1)?.let { focusOn(CellFocus(it, countField(state, it))) }
            }
        },
        // Completes the focused set and moves on to the next set's reps. A timed set runs its timer.
        onComplete = {
            val cell = focus ?: return@KeypadKeys
            val set = state.blocks.set(cell.ref) ?: return@KeypadKeys
            // A timed set, done or not, runs its timer (again); while it runs, ✓ completes it
            if (set.values.mode == SetMode.Time && state.setTimer?.ref != cell.ref) {
                viewModel.onStartTimerAt(cell.ref)
            } else {
                viewModel.onCompleteSetAt(cell.ref)
                val next = viewModel.state.value
                focusOn(next.current?.let { CellFocus(it, countField(next, it)) })
            }
        },
        onMode = { mode ->
            val cell = focus ?: return@KeypadKeys
            viewModel.onSetMode(cell.ref, mode)
            if (cell.field != ValueField.Weight) focusOn(CellFocus(cell.ref, if (mode == SetMode.Time) ValueField.Time else ValueField.Reps))
        },
        onHide = { focusOn(null) },
        // Back to the set being worked on
        onDone = {
            focusOn(null)
            selected = state.current ?: selected
        },
    ).revealing()

    AppScreen(
        topBar = { TableHeader(state, viewModel) },
        footer = {
            if (state.editMode) {
                // The tip as the footer's note, over Done and Add exercise
                AppFooter(
                    header = EditHint,
                    actions = listOf(
                        AppFooterAction(text = "Done", onClick = viewModel::onEditDone),
                        AppFooterAction(text = "Add exercise", onClick = viewModel::onAddExercise, style = AppFooterAction.Style.Secondary),
                    ),
                )
            } else {
                TableFooter(
                    state = state,
                    viewModel = viewModel,
                    focus = focus,
                    keys = keys,
                    // The set the dock's main action completes (or starts): the selected one, or else the current
                    target = selected?.takeIf { state.blocks.set(it)?.done == false } ?: state.current,
                    onShowKeypad = {
                        (selected ?: state.current)?.let { focusOn(CellFocus(it, countField(state, it))) }
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.editMode) {
                // Reordering and deleting exercises: the Spine and Dock prototype's edit view
                EditList(state, viewModel, contentPadding, showHint = false)
            } else if (state.blocks.isEmpty()) {
                EmptyTable(contentPadding, viewModel)
            } else {
                WorkoutTable(
                    state = state,
                    focus = focus,
                    selected = selected,
                    typed = typed,
                    reveal = reveal,
                    contentPadding = contentPadding,
                    onCellTapped = { focusOn(it) },
                    onSelect = { select(it) },
                    viewModel = viewModel,
                )
            }
            WorkoutSnackbarHost(snackbar, contentPadding)
        }
    }

    WorkoutDialogs(state, viewModel)
}

// Close, the workout's time with its name under it, the workout's options and Finish
@Composable
private fun TableHeader(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIconButton(onClick = viewModel::onDiscard, icon = ImageVector.vectorResource(R.drawable.ic_remove), contentDescription = "Close")
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = formatClock(state.elapsedSeconds), style = numberStyle().copy(fontSize = 22.sp), maxLines = 1)
            AppText(text = state.name, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
        }
        WorkoutOptionsButton(state, viewModel)
        // Filled once every set is done: it's what's next
        when {
            state.blocks.isEmpty() || state.editMode -> Unit
            state.unfinishedSets() == 0 -> AppButton(onClick = viewModel::onFinish, text = "Finish")
            else -> AppSecondaryButton(onClick = viewModel::onFinish, text = "Finish")
        }
    }
}

// The empty workout: a faded example of what the table becomes, which can't be touched. The dock has
// Add exercise.
@Composable
private fun EmptyTable(contentPadding: PaddingValues, viewModel: WorkoutPrototypeViewModel) {
    val example = remember {
        val values = SetValues(SetMode.Reps, reps = 12, timeSeconds = 30, weightKg = 20f)
        val block = PrototypeBlock(
            id = -1,
            exercises = listOf(PrototypeExercise(-2, "Example exercise")),
            rounds = listOf(
                PrototypeRound(-3, listOf(PrototypeSet(values, done = true, lastTime = values.copy(reps = 11)))),
                PrototypeRound(-4, listOf(PrototypeSet(values, lastTime = values))),
                PrototypeRound(-5, listOf(PrototypeSet(values, lastTime = values))),
            ),
        )
        val now = System.currentTimeMillis()
        WorkoutPrototypeState(name = "", startedAt = now, now = now, blocks = listOf(block), current = SetRef(-1, -4, 0))
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AppText(
            modifier = Modifier
                .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small)
                .clip(Theme.shapes.button)
                .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
                .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
            text = "EXAMPLE · NOT YOUR DATA",
            style = tagStyle(),
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
                    state = example,
                    focus = null,
                    selected = example.current,
                    typed = null,
                    reveal = 0,
                    contentPadding = contentPadding,
                    onCellTapped = {},
                    onSelect = {},
                    viewModel = viewModel,
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

// Reps or time, whichever the set records
internal fun countField(state: WorkoutPrototypeState, ref: SetRef): ValueField =
    if (state.blocks.set(ref)?.values?.mode == SetMode.Time) ValueField.Time else ValueField.Reps

private fun maxLength(field: ValueField) = when (field) {
    ValueField.Weight -> 6
    ValueField.Reps -> 3
    ValueField.Time -> 4
}

// Time is typed like a microwave: the last two digits are seconds, so 130 is 1:30
private fun parse(field: ValueField, text: String): Float = when (field) {
    ValueField.Weight -> text.toFloatOrNull() ?: 0f
    ValueField.Reps -> (text.toIntOrNull() ?: 0).toFloat()
    ValueField.Time -> typedSeconds(text).toFloat()
}

internal fun typedSeconds(text: String): Int {
    val number = text.toIntOrNull() ?: return 0
    return if (text.length <= 2) number else (number / 100) * 60 + number % 100
}

// A value as its digits, for backspacing into it
private fun rawText(values: com.everlog.ui.activities.workoutprototype.SetValues, field: ValueField): String = when (field) {
    ValueField.Weight -> formatWeight(values.weightKg)
    ValueField.Reps -> "${values.reps}"
    ValueField.Time -> {
        val minutes = values.timeSeconds / 60
        val seconds = values.timeSeconds % 60
        if (minutes > 0) "$minutes%02d".format(seconds) else "$seconds"
    }
}
