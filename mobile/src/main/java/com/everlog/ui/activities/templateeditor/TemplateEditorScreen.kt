package com.everlog.ui.activities.templateeditor

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutExercise
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.ExampleTable
import com.everlog.ui.components.exercisetable.ExerciseTab
import com.everlog.ui.components.exercisetable.ExerciseTable
import com.everlog.ui.components.exercisetable.ExerciseTableActions
import com.everlog.ui.components.exercisetable.TableColumns
import com.everlog.ui.components.exercisetable.TableView
import com.everlog.ui.components.exercisetable.exerciseName
import com.everlog.ui.components.exercisetable.focusLabel
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppCircularProgressIndicator
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSnackbarHost
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.keypad.AppKeypad
import com.everlog.ui.design.elements.keypad.AppKeypadAction
import com.everlog.ui.design.elements.keypad.AppKeypadEntry
import com.everlog.ui.design.elements.keypad.AppKeypadInput
import com.everlog.ui.design.elements.keypad.AppKeypadNote
import com.everlog.ui.design.elements.keypad.AppKeypadToggle
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.format.formatClock
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

// Templates hold targets: no last time, weight or ticks
private val TemplateColumns = TableColumns(lastTime = false, weight = false, tick = false)

// The unit isn't shown, as there's no weight column
private val NoWeights = SettingsManager.WeightUnit.KILOGRAM

/**
 * Renders the view-model's state and forwards what's tapped and typed to it.
 */
@Composable
internal fun TemplateEditorScreen(
    viewModel: TemplateEditorViewModel,
    onAddExercise: () -> Unit,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
    onExit: (TemplateEditorExit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

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
            override fun onAddSet(blockId: Long) = viewModel.onAddSet(blockId)
            override fun onDeleteSet(ref: SetRef) = viewModel.onDeleteSet(ref)
            override fun onOpenExercise(exercise: ELExercise, tab: ExerciseTab) = onOpenExercise(exercise, tab)
            override fun onDeleteExercise(blockId: Long) = viewModel.onDeleteExercise(blockId)
        }
    }
    val ready = state.load == TemplateLoad.Ready
    val empty = state.blocks.isEmpty()

    AppScreen(
        topBar = { Header(state, viewModel) },
        footer = {
            if (ready) {
                val addExercise = AppKeypadAction(stringResource(R.string.workout_log_add_exercise), onClick = onAddExercise)
                AppKeypad(
                    modifier = Modifier
                        .testTag(TemplateEditorTags.Keypad)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small),
                    entry = keypadEntry(state, viewModel),
                    input = input,
                    onHide = viewModel::onHideKeypad,
                    onShow = if (empty) null else viewModel::onShowKeypad,
                    primaryAction = addExercise,
                    strips = {
                        if (empty) AppKeypadNote(text = stringResource(R.string.template_editor_empty_hint))
                        state.timeForAllSets()?.let { seconds ->
                            AppKeypadNote(
                                modifier = Modifier.testTag(TemplateEditorTags.TimeForAllSets),
                                text = stringResource(R.string.template_editor_same_time, formatClock(seconds.toLong())),
                                action = AppKeypadAction(stringResource(R.string.template_editor_same_time_apply), onClick = viewModel::onTimeForAllSets),
                            )
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.load) {
                TemplateLoad.Loading -> AppCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                TemplateLoad.Failed -> LoadFailed(stringResource(R.string.template_editor_load_failed), onRetry = viewModel::onRetry)
                TemplateLoad.NotFound -> LoadFailed(stringResource(R.string.template_editor_not_found), onRetry = null)
                TemplateLoad.Ready -> if (empty) {
                    Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
                        TemplateSettings(state, viewModel, Modifier.padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.medium))
                        ExampleTable(
                            blocks = exampleBlocks(stringResource(R.string.workout_log_example_exercise)),
                            view = TableView(unit = NoWeights, unitLabel = "", selected = SetRef(-1, -2, 0), columns = TemplateColumns),
                            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        )
                    }
                } else {
                    ExerciseTable(
                        blocks = state.blocks,
                        view = TableView(
                            unit = NoWeights,
                            unitLabel = "",
                            focus = state.focus,
                            selected = state.focus?.ref,
                            typed = state.typed,
                            columns = TemplateColumns,
                        ),
                        reveal = state.reveal,
                        contentPadding = contentPadding,
                        actions = tableActions,
                        header = { TemplateSettings(state, viewModel, Modifier.padding(bottom = Theme.spacing.extraLarge)) },
                    )
                }
            }
            Messages(state, viewModel, Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding()))
        }
    }

    TemplateEditorDialogs(state, viewModel)
}

@Composable
private fun Header(state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Theme.spacing.extraSmall, end = Theme.spacing.large, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIconButton(
            onClick = viewModel::onClose,
            icon = ImageVector.vectorResource(R.drawable.ic_remove),
            contentDescription = stringResource(R.string.template_editor_close),
        )
        AppText(
            modifier = Modifier.weight(1f),
            text = stringResource(if (state.isNew) R.string.template_editor_new else R.string.template_editor_edit),
            style = Theme.typography.heading,
            maxLines = 1,
        )
        if (state.load == TemplateLoad.Ready) {
            AppButton(
                modifier = Modifier.testTag(TemplateEditorTags.Save),
                onClick = viewModel::onSave,
                text = stringResource(R.string.save),
                enabled = !state.saving,
            )
        }
    }
}

// The template's name and rest, above its exercises
@Composable
private fun TemplateSettings(state: TemplateEditorState, viewModel: TemplateEditorViewModel, modifier: Modifier) {
    AppListGroup(modifier = modifier) {
        row {
            AppListItem(
                modifier = Modifier.testTag(TemplateEditorTags.Name),
                title = stringResource(R.string.template_editor_name),
                subtitle = state.name,
                onClick = viewModel::onNameTapped,
            )
        }
        row {
            AppListItem(
                modifier = Modifier.testTag(TemplateEditorTags.Rest),
                title = stringResource(R.string.template_editor_rest),
                subtitle = formatClock(state.restSeconds.toLong()),
                onClick = viewModel::onRestTapped,
            )
        }
    }
}

@Composable
private fun keypadEntry(state: TemplateEditorState, viewModel: TemplateEditorViewModel): AppKeypadEntry? {
    val focus = state.focus ?: return null
    val set = state.blocks.set(focus.ref) ?: return null
    val mainKey = state.mainKey() ?: return null
    return AppKeypadEntry(
        label = focusLabel(state.blocks, focus, NoWeights),
        title = exerciseName(state.blocks, focus.ref),
        decimal = false,
        toggle = AppKeypadToggle(
            options = listOf(stringResource(R.string.workout_log_reps), stringResource(R.string.workout_log_time)),
            selected = if (set.mode == SetMode.Time) 1 else 0,
            onSelect = { viewModel.onModeSelected(if (it == 1) SetMode.Time else SetMode.Reps) },
        ),
        mainKey = AppKeypadAction(
            text = stringResource(
                when (mainKey) {
                    TemplateMainKey.NextSet -> R.string.template_editor_next_set
                    TemplateMainKey.Done -> R.string.workout_log_done
                },
            ),
            onClick = viewModel::onMainKey,
        ),
    )
}

@Composable
private fun Messages(state: TemplateEditorState, viewModel: TemplateEditorViewModel, modifier: Modifier) {
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
        is TemplateEditorMessage.TooManySets -> stringResource(R.string.exercises_max_sets, it.max)
        TemplateEditorMessage.SaveFailed -> stringResource(R.string.template_editor_save_failed)
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

// Couldn't load, with Try again, or deleted, with only the header's close
@Composable
private fun LoadFailed(text: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(text = text, color = Theme.contentColors.secondary, textAlign = TextAlign.Center)
        if (onRetry != null) AppButton(onClick = onRetry, text = stringResource(R.string.template_editor_retry))
    }
}

private fun exampleBlocks(name: String) = listOf(
    WorkoutBlock(
        id = -1,
        uuid = null,
        type = null,
        restSeconds = 0,
        exercises = listOf(WorkoutExercise(null, ELExercise(name = name))),
        rounds = (2L..4L).map { WorkoutRound(-it, listOf(WorkoutSet(reps = 10))) },
    ),
)
