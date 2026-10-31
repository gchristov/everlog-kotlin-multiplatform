package com.everlog.ui.activities.templateeditor

import com.everlog.android.common.design.R as DesignR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.android.common.compose.elements.AppSectionHeader
import com.everlog.android.common.compose.elements.AppTertiaryButton
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.common.compose.components.reorder.rememberReorderState
import com.everlog.android.common.compose.components.reorder.reorderGrip
import com.everlog.android.common.compose.components.reorder.reorderItem
import com.everlog.android.common.compose.components.reorder.reorderViewport
import com.everlog.android.common.kotlin.format.formatClock

/**
 * The template's details, then its exercises to edit in place: drag one by its grip (every exercise shrinks to its
 * name while it's dragged), tap a value to type, delete a set with its bin, and under each exercise add a set, make it
 * a superset with the next or split its group.
 */
@Composable
internal fun TemplateEditorList(
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    contentPadding: PaddingValues,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    val scroll = rememberScrollState()
    val bottom = contentPadding.calculateBottomPadding()
    val reorder = rememberReorderState(scroll, state.blocks.map { it.id }, bottom, viewModel::onMoveExercise)
    val dragging = reorder.dragging != null
    val keepClear = with(LocalDensity.current) { (bottom + Theme.spacing.extraLarge).toPx() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TemplateEditorTags.List)
            .reorderViewport(reorder)
            .verticalScroll(scroll)
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = bottom + Theme.spacing.large),
    ) {
        TemplateDetails(state, viewModel)
        ExercisesHeader()
        state.blocks.forEachIndexed { index, block ->
            key(block.id) {
                Column(modifier = Modifier.testTag(TemplateEditorTags.exercise(index)).reorderItem(reorder, block.id)) {
                    TemplateExercise(
                        index = index,
                        block = block,
                        state = state,
                        viewModel = viewModel,
                        collapsed = dragging,
                        lifted = reorder.dragging == block.id,
                        grip = Modifier
                            .testTag(TemplateEditorTags.grip(index))
                            .reorderGrip(reorder, block.id, onStart = viewModel::onDragStarted),
                        keepClear = keepClear,
                        onOpenExercise = onOpenExercise,
                    )
                    if (dragging) {
                        Spacer(modifier = Modifier.height(Theme.spacing.medium))
                    } else {
                        ExerciseActions(
                            index = index,
                            onAddSet = { viewModel.onAddSet(block.id) },
                            onJoin = if (index < state.blocks.lastIndex) ({ viewModel.onJoinWithNext(block.id) }) else null,
                            onSplit = if (block.exercises.size > 1) ({ viewModel.onSplitGroup(block.id) }) else null,
                        )
                    }
                }
            }
        }
    }
}

// The name and rest, each value first with what it is above it
@Composable
internal fun TemplateDetails(state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    AppListGroup(header = stringResource(R.string.template_editor_details)) {
        row {
            AppListItem(
                modifier = Modifier.testTag(TemplateEditorTags.Name),
                header = stringResource(R.string.template_editor_name),
                title = state.name,
                onClick = viewModel::onNameTapped,
            )
        }
        row {
            AppListItem(
                modifier = Modifier.testTag(TemplateEditorTags.Rest),
                header = stringResource(R.string.template_editor_rest),
                title = formatClock(state.restSeconds.toLong()),
                onClick = viewModel::onRestTapped,
            )
        }
    }
}

@Composable
internal fun ExercisesHeader() {
    Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
    AppSectionHeader(modifier = Modifier.padding(bottom = Theme.spacing.large), text = stringResource(R.string.template_editor_exercises))
}

// Under an exercise's sets, as the workout's Add set: Superset with next unless it's the last, Split when it's a group
@Composable
internal fun ExerciseActions(index: Int, onAddSet: () -> Unit, onJoin: (() -> Unit)?, onSplit: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = cardIndent(), bottom = Theme.spacing.large),
        horizontalArrangement = Arrangement.Center,
    ) {
        // Narrower than a lone button's, so all three fit on a small phone
        val padding = PaddingValues(horizontal = Theme.spacing.medium)
        AppTertiaryButton(
            modifier = Modifier.testTag(TemplateEditorTags.addSet(index)),
            onClick = onAddSet,
            text = stringResource(DesignR.string.workout_log_add_set),
            contentPadding = padding,
        )
        if (onJoin != null) {
            AppTertiaryButton(
                modifier = Modifier.testTag(TemplateEditorTags.join(index)),
                onClick = onJoin,
                text = stringResource(R.string.template_editor_superset_with_next),
                contentPadding = padding,
            )
        }
        if (onSplit != null) {
            AppTertiaryButton(
                modifier = Modifier.testTag(TemplateEditorTags.split(index)),
                onClick = onSplit,
                text = stringResource(R.string.template_editor_split),
                contentPadding = padding,
            )
        }
    }
}
