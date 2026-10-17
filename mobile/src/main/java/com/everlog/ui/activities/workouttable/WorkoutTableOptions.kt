package com.everlog.ui.activities.workouttable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.everlog.ui.activities.workoutprototype.DockPill
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeViewModel
import com.everlog.ui.activities.workoutprototype.formatClock
import com.everlog.ui.activities.workoutprototype.numberStyle
import com.everlog.ui.design.elements.AppCheck
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.launch

private object OptionsSizes {
    val RestValue = 56.dp
    val StepButton = 36.dp
}

/**
 * The workout's options, from the dock's cog: a sheet over the dimmed workout, with room for options
 * that need more than a line (the rest timer's stepper now; sound, notes and views later). Rows are
 * the design system's list groups. The design system has no bottom sheet yet; this is a candidate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutOptionsSheet(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Closes the sheet, then does what was picked
    val pick: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.backgrounds.primary,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.large)
                .padding(bottom = Theme.spacing.large)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            AppText(text = "Workout options", style = Theme.typography.heading)
            AppListGroup {
                row {
                    AppListItem(
                        title = "Rest timer",
                        subtitle = "After each set",
                        trailing = {
                            RestStepper(
                                seconds = state.restSeconds,
                                onStep = { viewModel.onRestSecondsStep(it * WorkoutPrototypeViewModel.RestStepSeconds) },
                            )
                        },
                    )
                }
                row {
                    AppListItem(
                        title = "Weight suggestions",
                        subtitle = "Start sets on a suggested weight",
                        trailing = { AppCheck(checked = state.suggestions) },
                        onClick = viewModel::onToggleSuggestions,
                    )
                }
                if (state.blocks.isNotEmpty()) {
                    row {
                        AppListItem(
                            title = "Edit workout",
                            subtitle = "Reorder or delete exercises",
                            onClick = { pick(viewModel::onEditWorkout) },
                        )
                    }
                }
            }
            AppListGroup {
                row {
                    AppListItem(
                        title = "Discard workout",
                        titleColor = Theme.contentColors.destructive,
                        onClick = { pick(viewModel::onDiscard) },
                    )
                }
            }
            // Only in the prototype, to try both states
            AppListGroup(header = "Prototype") {
                row { AppListItem(title = "Reset to sample", onClick = { pick(viewModel::onResetToSample) }) }
                row { AppListItem(title = "Start empty", onClick = { pick(viewModel::onStartEmpty) }) }
                row {
                    AppListItem(
                        title = "Later ideas",
                        subtitle = "Set effort and comparison with last time",
                        trailing = { AppCheck(checked = state.laterIdeas) },
                        onClick = viewModel::onToggleLaterIdeas,
                    )
                }
            }
        }
    }
}

// −10s, the rest, +10s
@Composable
private fun RestStepper(seconds: Int, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        DockPill(text = "−10s", onClick = { onStep(-1) }, height = OptionsSizes.StepButton, horizontalPadding = Theme.spacing.medium)
        AppText(
            modifier = Modifier.widthIn(min = OptionsSizes.RestValue),
            text = formatClock(seconds.toLong()),
            style = numberStyle().copy(fontSize = Theme.typography.heading.fontSize),
            textAlign = TextAlign.Center,
        )
        DockPill(text = "+10s", onClick = { onStep(1) }, height = OptionsSizes.StepButton, horizontalPadding = Theme.spacing.medium)
    }
}
