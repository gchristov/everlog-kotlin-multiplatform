package com.everlog.ui.activities.workoutlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.R
import com.everlog.ui.design.elements.AppBottomSheet
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.format.formatClock

/**
 * The cog in the header, with the rest after each set, Edit workout and Discard.
 */
@Composable
internal fun WorkoutOptionsButton(state: WorkoutLogState, viewModel: WorkoutLogViewModel) {
    var open by remember { mutableStateOf(false) }
    AppIconButton(
        onClick = { open = true },
        icon = ImageVector.vectorResource(R.drawable.ic_settings),
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

@Composable
internal fun RestStepper(seconds: Int, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        AppIconButton(
            onClick = { onStep(-1) },
            icon = ImageVector.vectorResource(R.drawable.ic_minus),
            contentDescription = stringResource(R.string.workout_log_rest_less),
            outlined = true,
        )
        AppText(text = formatClock(seconds.toLong()), style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"))
        AppIconButton(
            onClick = { onStep(1) },
            icon = ImageVector.vectorResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.workout_log_rest_more),
            outlined = true,
        )
    }
}
