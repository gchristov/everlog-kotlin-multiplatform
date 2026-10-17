package com.everlog.ui.activities.workoutprototype

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppMenuItem
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

// Debug only, from Settings › Developer: the Spine and Dock workout screen from the Everlog design
// ("Workout screen" page), on mock data, to try on a phone next to the current workout screen.
// Nothing is saved.
class WorkoutPrototypeActivity : CommonComposeActivity() {
    private val viewModel by viewModels<WorkoutPrototypeViewModel> {
        createViewModelFactory { WorkoutPrototypeViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = WorkoutPrototypeScreen(viewModel = viewModel, onClose = ::finish)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutPrototypeScreen(viewModel: WorkoutPrototypeViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var focusOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    KeepScreenOn()

    BackHandler {
        when {
            state.editMode -> viewModel.onEditDone()
            state.editing != null -> viewModel.onCancelEdit()
            else -> viewModel.onDiscard()
        }
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

    LaunchedEffect(state.deletedRound) {
        if (state.deletedRound == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(message = "Set deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete() else viewModel.onUndoGone()
    }

    val spineActions = remember(viewModel) {
        SpineActions(
            onSetTapped = viewModel::onSetTapped,
            onAddSet = viewModel::onAddSet,
            onDeleteRound = viewModel::onDeleteRound,
            onHistory = viewModel::onHistory,
            onDeleteBlock = viewModel::onDeleteBlock,
            onToggleRound = viewModel::onToggleRound,
            onExerciseDetails = viewModel::onExerciseDetails,
        )
    }

    AppScreen(
        topBar = { WorkoutHeader(state, viewModel) },
        footer = {
            if (state.editMode) {
                // Outlined: Done in the header is the filled button
                AppFooter(actions = listOf(AppFooterAction(text = "Add exercise", onClick = viewModel::onAddExercise, style = AppFooterAction.Style.Secondary)))
            } else {
                Dock(state, DockActions(viewModel, onPullUp = { focusOpen = true }, menuItems = workoutMenuItems(state, viewModel)))
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.editMode -> EditList(state, viewModel, contentPadding)
                state.blocks.isEmpty() -> GhostSpine(contentPadding)
                else -> SpineList(state = state, actions = spineActions, contentPadding = contentPadding)
            }
            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding() + Theme.spacing.small),
                hostState = snackbar,
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(horizontal = Theme.spacing.large),
                    shape = Theme.shapes.surface,
                    containerColor = Theme.backgrounds.surfaceRaised,
                    contentColor = Theme.contentColors.primary,
                    actionColor = Theme.contentColors.action,
                )
            }
        }
    }

    Dialogs(state, viewModel)

    if (focusOpen) {
        ModalBottomSheet(onDismissRequest = { focusOpen = false }, containerColor = Theme.backgrounds.surface) {
            Column(modifier = Modifier.padding(horizontal = Theme.spacing.large)) {
                AppText(text = "Focus", style = Theme.typography.heading)
                Spacer(modifier = Modifier.height(Theme.spacing.small))
                AppText(
                    text = "Focus isn't designed yet. It will show one exercise on the whole screen, with a large image, the current set and rest. View options will live here too.",
                    color = Theme.contentColors.secondary,
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
            }
        }
    }
}

// Close, the workout's time with its name under it, and Finish (Done while editing). The workout's
// options are in the dock.
@Composable
private fun WorkoutHeader(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Closing throws the workout away, so it asks first, like Back
        AppIconButton(
            onClick = viewModel::onDiscard,
            icon = ImageVector.vectorResource(R.drawable.ic_remove),
            contentDescription = "Close",
        )
        Column(modifier = Modifier.weight(1f).padding(start = Theme.spacing.extraSmall)) {
            AppText(text = formatClock(state.elapsedSeconds), style = numberStyle().copy(fontSize = 22.sp), maxLines = 1)
            AppText(text = state.name, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
        }
        when {
            state.editMode -> AppButton(modifier = Modifier.padding(start = Theme.spacing.small), onClick = viewModel::onEditDone, text = "Done")
            // Outlined, so Complete set is the one filled button on the screen
            state.blocks.isNotEmpty() -> AppSecondaryButton(modifier = Modifier.padding(start = Theme.spacing.small), onClick = viewModel::onFinish, text = "Finish")
        }
    }
}

private fun workoutMenuItems(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) = buildList {
    if (state.blocks.isNotEmpty() && !state.editMode) add(AppMenuItem(text = "Edit workout", onClick = viewModel::onEditWorkout))
    add(AppMenuItem(text = "Rest timer · ${formatClock(state.restSeconds.toLong())}", onClick = viewModel::onRestTimerSettings))
    add(AppMenuItem(text = "Discard workout", onClick = viewModel::onDiscard))
    // Prototype only, to try both states
    add(AppMenuItem(text = "Reset to sample", onClick = viewModel::onResetToSample))
    add(AppMenuItem(text = "Start empty", onClick = viewModel::onStartEmpty))
}

@Composable
private fun Dialogs(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        is WorkoutDialog.Finish -> AppDialog(
            title = "Finish workout?",
            body = if (dialog.unfinishedSets == 1) "1 set isn't done. It'll be left out." else "${dialog.unfinishedSets} sets aren't done. They'll be left out.",
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction("Finish", viewModel::onFinishConfirmed),
            secondaryAction = AppDialogAction("Keep going", viewModel::onDialogDismissed),
        )
        is WorkoutDialog.DeleteBlock -> {
            val name = state.block(dialog.blockId)?.exercises?.joinToString(" + ") { it.name }.orEmpty()
            AppDialog(
                title = "Delete $name?",
                body = "Its sets go with it.",
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction("Delete") { viewModel.onDeleteBlockConfirmed(dialog.blockId) },
                secondaryAction = AppDialogAction("Cancel", viewModel::onDialogDismissed),
            )
        }
        WorkoutDialog.Discard -> AppDialog(
            title = "Discard workout?",
            body = "Nothing from this workout will be kept.",
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction("Discard", viewModel::onDiscardConfirmed),
            secondaryAction = AppDialogAction("Keep going", viewModel::onDialogDismissed),
        )
        WorkoutDialog.RestTimer -> AppDialog(
            title = "Rest timer",
            body = "One rest for the whole workout, after each set.",
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction("Done", viewModel::onDialogDismissed),
        ) {
            Stepper(
                value = formatClock(state.restSeconds.toLong()),
                label = "rest",
                onStep = { viewModel.onRestSecondsStep(it * WorkoutPrototypeViewModel.RestStepSeconds) },
                onValueClick = {},
            )
        }
        is WorkoutDialog.EnterValue -> EnterValueDialog(state, dialog.field, viewModel)
    }
}

// Typing a value instead of stepping to it, e.g. a jump from 20 to 100 kg
@Composable
private fun EnterValueDialog(state: WorkoutPrototypeState, field: ValueField, viewModel: WorkoutPrototypeViewModel) {
    val values = state.editing?.values ?: state.current?.let { state.blocks.set(it)?.values } ?: return
    val initial = when (field) {
        ValueField.Reps -> "${values.reps}"
        ValueField.Time -> "${values.timeSeconds}"
        ValueField.Weight -> formatWeight(values.weightKg)
    }
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val submit = { text.replace(',', '.').toFloatOrNull()?.let { viewModel.onValueEntered(field, it) } ?: viewModel.onDialogDismissed() }
    AppDialog(
        title = when (field) {
            ValueField.Reps -> "Reps"
            ValueField.Time -> "Time in seconds"
            ValueField.Weight -> "Weight in kg"
        },
        onDismissRequest = viewModel::onDialogDismissed,
        primaryAction = AppDialogAction("Set") { submit() },
        secondaryAction = AppDialogAction("Cancel", viewModel::onDialogDismissed),
    ) {
        OutlinedTextField(
            modifier = Modifier.focusRequester(focus),
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (field == ValueField.Weight) KeyboardType.Decimal else KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Theme.contentColors.primary,
                unfocusedTextColor = Theme.contentColors.primary,
                focusedBorderColor = Theme.contentColors.action,
                unfocusedBorderColor = Theme.backgrounds.separator,
                cursorColor = Theme.contentColors.action,
            ),
        )
    }
    LaunchedEffect(Unit) {
        awaitFrame()
        focus.requestFocus()
    }
}

// The screen stays on through a workout, like the current workout screen can
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

// When a rest or a set timer ends
private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    val pattern = longArrayOf(0, 180, 120, 180)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}
