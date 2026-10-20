package com.everlog.ui.activities.workoutlog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.data.controllers.workoutsession.block
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction

@Composable
internal fun WorkoutLogDialogs(state: WorkoutLogState, viewModel: WorkoutLogViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        WorkoutLogDialog.Finish -> AppDialog(
            title = stringResource(R.string.workout_log_finish_title),
            body = stringResource(R.string.workout_log_finish_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.workout_finish), viewModel::onFinishConfirmed),
            secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
        )
        WorkoutLogDialog.RestTimer -> AppDialog(
            title = stringResource(R.string.workout_log_rest_timer),
            body = stringResource(R.string.workout_log_rest_timer_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.workout_log_done), viewModel::onDialogDismissed),
        ) {
            RestStepper(seconds = state.restSeconds, onStep = viewModel::onRestStep)
        }
        is WorkoutLogDialog.DeleteExercise -> {
            val name = state.blocks.block(dialog.blockId)?.exercises?.joinToString(" + ") { it.exercise?.name.orEmpty() }.orEmpty()
            AppDialog(
                title = stringResource(R.string.workout_log_delete_exercise_title, name),
                body = stringResource(R.string.workout_log_delete_exercise_body),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(R.string.workout_log_delete)) { viewModel.onDeleteExerciseConfirmed(dialog.blockId) },
                secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
            )
        }
        is WorkoutLogDialog.Discard -> if (dialog.setsCompleted > 0) {
            // Finish is the main action, so logged sets aren't lost by accident
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
                tertiaryAction = AppDialogAction(stringResource(R.string.workout_discard_keep_going), viewModel::onDialogDismissed),
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
