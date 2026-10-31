package com.everlog.android.workout.log

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.android.common.compose.elements.AppBottomSheet
import com.everlog.android.common.compose.elements.AppIconButton
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.workout.setsuggestions.SetSuggestion
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.workout.R
import com.everlog.android.common.design.R as DesignR

/**
 * The cog in the header, with the rest after each set, Set suggestions, Edit workout, Send feedback and Discard.
 */
@Composable
internal fun WorkoutOptionsButton(
    state: WorkoutLogState,
    viewModel: WorkoutLogViewModel,
    onOpenSetSuggestions: () -> Unit,
    onSendFeedback: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    AppIconButton(
        // As tall as the Finish button beside it
        modifier = Modifier.size(Theme.sizes.button),
        onClick = { open = true },
        icon = ImageVector.vectorResource(DesignR.drawable.ic_settings),
        contentDescription = stringResource(R.string.workout_log_options),
        outlined = true,
    )
    if (!open) return
    AppBottomSheet(onDismissRequest = { open = false }, title = stringResource(R.string.workout_log_options)) {
        AppListGroup {
            row {
                AppListItem(
                    title = stringResource(R.string.workout_log_rest_timer),
                    subtitle = stringResource(R.string.workout_log_rest_timer_subtitle, formatClock(state.restSeconds.toLong())),
                    onClick = { dismiss(viewModel::onRestTimerSettings) },
                )
            }
            row {
                AppListItem(
                    modifier = Modifier.testTag(WorkoutLogTags.SetSuggestionsOption),
                    title = stringResource(DesignR.string.set_suggestions_title),
                    subtitle = SetSuggestion.of(state.suggestions)?.let { stringResource(it.title) } ?: state.suggestions.valueName(LocalContext.current),
                    onClick = { dismiss(onOpenSetSuggestions) },
                )
            }
            if (state.blocks.isNotEmpty()) {
                row {
                    AppListItem(
                        title = stringResource(R.string.workout_log_edit),
                        subtitle = stringResource(R.string.workout_log_edit_subtitle),
                        onClick = { dismiss(viewModel::onEditWorkout) },
                    )
                }
            }
        }
        if (onSendFeedback != null) {
            AppListGroup {
                row {
                    AppListItem(
                        modifier = Modifier.testTag(WorkoutLogTags.SendFeedbackOption),
                        title = stringResource(DesignR.string.feedback_send),
                        subtitle = stringResource(R.string.feedback_workout_subtitle),
                        onClick = { dismiss(onSendFeedback) },
                    )
                }
            }
        }
        AppListGroup {
            row {
                AppListItem(
                    title = stringResource(R.string.workout_log_discard),
                    titleColor = Theme.contentColors.destructive,
                    onClick = { dismiss(viewModel::onClose) },
                )
            }
        }
    }
}
