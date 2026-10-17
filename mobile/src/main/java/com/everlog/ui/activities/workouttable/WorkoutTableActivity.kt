package com.everlog.ui.activities.workouttable

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import com.everlog.ui.activities.workoutprototype.WorkoutDialogs
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeViewModel
import com.everlog.ui.activities.workoutprototype.formatClock
import com.everlog.ui.activities.workoutprototype.formatWeight
import com.everlog.ui.activities.workoutprototype.numberStyle
import com.everlog.ui.activities.workoutprototype.outlinedCircle
import com.everlog.ui.activities.workoutprototype.refs
import com.everlog.ui.activities.workoutprototype.set
import com.everlog.ui.activities.workoutprototype.vibrate
import com.everlog.ui.activities.workoutprototype.workoutMenuItems
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppMenu
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
    // The highlighted set: the last one touched (row, cell, tick or ▶), and the keypad's while it's open.
    // Nothing until the first touch; once the selected set is completed, the next set.
    var selected by remember { mutableStateOf<SetRef?>(null) }
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    var typed by remember { mutableStateOf<String?>(null) }

    KeepScreenOn()

    BackHandler {
        if (focus != null) focus = null else viewModel.onDiscard()
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

    // A deleted set can't keep the keypad's focus
    LaunchedEffect(state.blocks) {
        focus?.let { if (state.blocks.set(it.ref) == null) focus = null }
    }

    fun focusOn(cell: CellFocus?) {
        focus = cell
        typed = null
        cell?.let { selected = it.ref }
    }

    // Touching a set's tick or ▶ selects it, and moves the keypad there if it's open
    fun select(ref: SetRef) {
        selected = ref
        if (focus != null) focusOn(CellFocus(ref, ValueField.Weight))
    }

    // The selected set got completed (its tick, ✓ or its timer ending): select the next one
    LaunchedEffect(state.current) {
        val current = selected ?: return@LaunchedEffect
        if (state.blocks.set(current)?.done == true) {
            selected = state.current
            if (focus != null) focusOn(state.current?.let { CellFocus(it, ValueField.Weight) })
        }
    }

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
        // kg, then reps (or time), then the next set's kg
        onNext = {
            val cell = focus ?: return@KeypadKeys
            if (cell.field == ValueField.Weight) {
                focusOn(CellFocus(cell.ref, countField(state, cell.ref)))
            } else {
                val refs = state.blocks.refs()
                refs.getOrNull(refs.indexOf(cell.ref) + 1)?.let { focusOn(CellFocus(it, ValueField.Weight)) }
            }
        },
        // Completes the focused set and moves on to the next set's kg. A timed set runs its timer.
        onComplete = {
            val cell = focus ?: return@KeypadKeys
            val set = state.blocks.set(cell.ref) ?: return@KeypadKeys
            // A timed set, done or not, runs its timer (again); while it runs, ✓ completes it
            if (set.values.mode == SetMode.Time && state.setTimer?.ref != cell.ref) {
                viewModel.onStartTimerAt(cell.ref)
            } else {
                viewModel.onCompleteSetAt(cell.ref)
                focusOn(viewModel.state.value.current?.let { CellFocus(it, ValueField.Weight) })
            }
        },
        onMode = { mode ->
            val cell = focus ?: return@KeypadKeys
            viewModel.onSetMode(cell.ref, mode)
            if (cell.field != ValueField.Weight) focusOn(CellFocus(cell.ref, if (mode == SetMode.Time) ValueField.Time else ValueField.Reps))
        },
        onHide = { focusOn(null) },
    )

    AppScreen(
        topBar = { TableHeader(state, viewModel) },
        footer = { TableFooter(state = state, viewModel = viewModel, focus = focus, keys = keys) },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.blocks.isEmpty()) {
                EmptyTable(onAddExercise = viewModel::onAddExercise)
            } else {
                WorkoutTable(
                    state = state,
                    focus = focus,
                    selected = selected,
                    typed = typed,
                    contentPadding = contentPadding,
                    onCellTapped = { focusOn(it) },
                    onSelect = { select(it) },
                    viewModel = viewModel,
                )
            }
        }
    }

    WorkoutDialogs(state, viewModel)
}

// Close, the workout's time with its name under it, the workout's options and Finish
@Composable
private fun TableHeader(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
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
        Box(modifier = Modifier.outlinedCircle()) {
            AppIconButton(
                onClick = { menuOpen = true },
                icon = ImageVector.vectorResource(R.drawable.ic_settings),
                contentDescription = "Workout options",
            )
            // No edit mode here: the table has no reorder view
            AppMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                items = workoutMenuItems(state, viewModel).filterNot { it.text == "Edit workout" },
            )
        }
        if (state.blocks.isNotEmpty()) AppSecondaryButton(onClick = viewModel::onFinish, text = "Finish")
    }
}

@Composable
private fun EmptyTable(onAddExercise: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Theme.spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(text = "No exercises yet", style = Theme.typography.heading)
        AppText(
            modifier = Modifier.padding(top = Theme.spacing.small, bottom = Theme.spacing.large),
            text = "Add one to start logging",
            color = Theme.contentColors.secondary,
        )
        AppSecondaryButton(onClick = onAddExercise, text = "Add exercise")
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
