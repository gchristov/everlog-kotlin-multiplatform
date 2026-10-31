package com.everlog.android.workout.feature.log

import com.everlog.android.common.design.R as DesignR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppDialog
import com.everlog.android.common.compose.elements.AppDialogAction
import com.everlog.android.common.session.block
import com.everlog.android.workout.feature.R
import com.everlog.android.common.compose.components.rest.RestStepper

@Composable
internal fun WorkoutLogDialogs(state: WorkoutLogState, viewModel: WorkoutLogViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        WorkoutLogDialog.Finish -> AppDialog(
            title = stringResource(R.string.workout_log_finish_title),
            body = stringResource(R.string.workout_log_finish_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(DesignR.string.workout_finish), viewModel::onFinishConfirmed),
            secondaryAction = AppDialogAction(stringResource(DesignR.string.cancel), viewModel::onDialogDismissed),
        )
        WorkoutLogDialog.RestTimer -> AppDialog(
            title = stringResource(R.string.workout_log_rest_timer),
            body = stringResource(R.string.workout_log_rest_timer_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(DesignR.string.workout_log_done), viewModel::onDialogDismissed),
        ) {
            RestStepper(seconds = state.restSeconds, onStep = viewModel::onRestStep)
        }
        is WorkoutLogDialog.DeleteExercise -> {
            val name = state.blocks.block(dialog.blockId)?.exercises?.joinToString(" + ") { it.exercise?.name.orEmpty() }.orEmpty()
            AppDialog(
                title = stringResource(R.string.workout_log_delete_exercise_title, name),
                body = stringResource(R.string.workout_log_delete_exercise_body),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(DesignR.string.workout_log_delete)) { viewModel.onDeleteExerciseConfirmed(dialog.blockId) },
                secondaryAction = AppDialogAction(stringResource(DesignR.string.cancel), viewModel::onDialogDismissed),
            )
        }
        is WorkoutLogDialog.Discard -> if (dialog.setsCompleted > 0) {
            // Finish is the main action, so logged sets aren't lost by accident
            var body = pluralStringResource(DesignR.plurals.workout_discard_sets_logged, dialog.setsCompleted, dialog.setsCompleted)
            if (dialog.untickedSaved > 0) {
                body += " " + pluralStringResource(DesignR.plurals.workout_discard_unticked_saved, dialog.untickedSaved, dialog.untickedSaved)
            }
            AppDialog(
                title = stringResource(DesignR.string.workout_discard_title),
                body = body,
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(DesignR.string.workout_discard_finish), viewModel::onDiscardFinish),
                secondaryAction = AppDialogAction(stringResource(DesignR.string.discard), viewModel::onDiscardConfirmed),
                tertiaryAction = AppDialogAction(stringResource(DesignR.string.workout_discard_keep_going), viewModel::onDialogDismissed),
            )
        } else {
            AppDialog(
                title = stringResource(DesignR.string.workout_discard_title),
                body = stringResource(DesignR.string.workout_discard_nothing_logged),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(DesignR.string.workout_discard_keep_going), viewModel::onDialogDismissed),
                secondaryAction = AppDialogAction(stringResource(DesignR.string.discard), viewModel::onDiscardConfirmed),
            )
        }
    }
}
