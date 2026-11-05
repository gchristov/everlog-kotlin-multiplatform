package com.everlog.android.template.editor

import com.everlog.android.common.design.R as DesignR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.workoutbuilder.data.WorkoutBlock
import com.everlog.android.workoutbuilder.data.WorkoutExercise
import com.everlog.android.workoutbuilder.data.WorkoutRound
import com.everlog.android.workoutbuilder.data.WorkoutSet


/**
 * A template with no exercises: its details, then a faded example exercise under an EXAMPLE label, which can't be
 * touched, showing what adding exercises gives.
 */
@Composable
internal fun TemplateEditorExample(state: TemplateEditorState, viewModel: TemplateEditorViewModel, contentPadding: PaddingValues) {
    val example = exampleBlocks(stringResource(DesignR.string.workout_log_example_exercise))
    val exampleState = state.copy(blocks = example, focus = null, selected = null, typed = null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
    ) {
        TemplateDetails(state, viewModel)
        ExercisesHeader()
        AppExampleLabel(modifier = Modifier.padding(bottom = Theme.spacing.medium))
        AppExample(modifier = Modifier.testTag(TemplateEditorTags.Example)) {
            Column {
                example.forEach { block ->
                    TemplateExercise(
                        index = -1,
                        block = block,
                        state = exampleState,
                        viewModel = viewModel,
                        collapsed = false,
                        lifted = false,
                        grip = Modifier,
                        keepClear = 0f,
                        onOpenExercise = { _, _ -> },
                    )
                    ExerciseActions(index = -1, onAddSet = {}, onJoin = {}, onSplit = null)
                }
            }
        }
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
