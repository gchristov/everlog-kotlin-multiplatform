package com.everlog.ui.activities.templateeditor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.everlog.R
import com.everlog.android.common.compose.theme.Theme
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutExercise
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.components.exercisetable.ExampleLabel
import com.everlog.ui.components.exercisetable.blockingTouches

private const val ExampleAlpha = 0.35f

/**
 * A template with no exercises: its details, then a faded example exercise under an EXAMPLE label, which can't be
 * touched, showing what adding exercises gives.
 */
@Composable
internal fun TemplateEditorExample(state: TemplateEditorState, viewModel: TemplateEditorViewModel, contentPadding: PaddingValues) {
    val example = exampleBlocks(stringResource(R.string.workout_log_example_exercise))
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
        ExampleLabel(modifier = Modifier.padding(bottom = Theme.spacing.medium))
        Box(modifier = Modifier.testTag(TemplateEditorTags.Example)) {
            // Faded as one layer, so overlapping parts don't show through each other
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = ExampleAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .clearAndSetSemantics {},
            ) {
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
            Box(modifier = Modifier.matchParentSize().blockingTouches())
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
