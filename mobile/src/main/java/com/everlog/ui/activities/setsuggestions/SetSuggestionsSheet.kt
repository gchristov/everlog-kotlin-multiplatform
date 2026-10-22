package com.everlog.ui.activities.setsuggestions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.ui.design.elements.AppBottomSheet
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.format.formatWeight
import com.everlog.utils.format.unitName

/**
 * What a 1RM is, how it's estimated and what a goal does with it, with [example] in the user's unit.
 */
@Composable
internal fun HowGoalWeightsSheet(example: GoalWeightExample, unit: WeightUnit, onDismiss: () -> Unit) {
    val unitName = unitName(unit)
    val set = stringResource(R.string.set_suggestions_how_set_value, formatWeight(example.weight), unitName, example.reps)
    val oneRepMax = stringResource(R.string.set_suggestions_how_weight_value, formatWeight(example.oneRepMax), unitName)
    val goalWeight = stringResource(R.string.set_suggestions_how_weight_value, formatWeight(example.goalWeight), unitName)
    AppBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.set_suggestions_how)) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppText(text = stringResource(R.string.set_suggestions_how_1rm), color = Theme.contentColors.secondary)
            AppText(text = stringResource(R.string.set_suggestions_how_goal), color = Theme.contentColors.secondary)
        }
        AppListGroup(
            header = stringResource(R.string.set_suggestions_how_example, stringResource(R.string.set_suggestions_how_example_exercise)),
            footer = stringResource(R.string.set_suggestions_how_rounding, unitName),
        ) {
            row { AppListItem(title = stringResource(R.string.set_suggestions_how_set), trailing = { Value(set) }) }
            row { AppListItem(title = stringResource(R.string.set_suggestions_how_estimate), trailing = { Value(oneRepMax) }) }
            row {
                AppListItem(
                    title = stringResource(R.string.set_suggestions_how_goal_row, stringResource(example.goal.title), example.goal.percent),
                    trailing = { Value(goalWeight, highlighted = true) },
                )
            }
        }
        AppButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SetSuggestionsTags.HowDone),
            onClick = { dismiss() },
            text = stringResource(R.string.set_suggestions_how_done),
        )
    }
}

@Composable
private fun Value(text: String, highlighted: Boolean = false) = AppText(
    text = text,
    style = if (highlighted) Theme.typography.bodyBold else Theme.typography.body,
    color = if (highlighted) Theme.contentColors.action else Theme.contentColors.primary,
)
