package com.everlog.android.settings.ui.tab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppBottomSheet
import com.everlog.android.common.compose.elements.AppBottomSheetScope
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppCheck
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppWheel
import com.everlog.android.common.compose.elements.AppWheelColumn
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.settings.ui.R
import java.text.DecimalFormatSymbols
import com.everlog.android.common.design.R as DesignR

@Composable
internal fun SettingsTabSheets(
    state: SettingsTabState,
    viewModel: SettingsTabViewModel,
    onLogOutConfirmed: () -> Unit,
) {
    when (val sheet = state.sheet) {
        null -> Unit
        SettingsTabSheet.FirstDayOfWeek -> AppBottomSheet(
            onDismissRequest = viewModel::onSheetDismissed,
            title = stringResource(R.string.settings_first_week_day),
        ) {
            ChoiceGroup(SettingsTabLimits.FirstDaysOfWeek, state.firstDayOfWeek, { it.displayName() }, viewModel::onFirstDayOfWeekPicked)
        }
        SettingsTabSheet.WeightUnit -> AppBottomSheet(
            onDismissRequest = viewModel::onSheetDismissed,
            title = stringResource(R.string.settings_unit_weight),
        ) {
            ChoiceGroup(WeightUnit.entries, state.weightUnit, { weightUnitName(it) }, viewModel::onWeightUnitPicked)
        }
        is SettingsTabSheet.WeeklyGoal -> AppBottomSheet(
            onDismissRequest = viewModel::onSheetDismissed,
            title = stringResource(DesignR.string.settings_weekly_goal),
        ) {
            val goals = SettingsTabLimits.WeeklyGoals.toList()
            val names = goals.map { pluralStringResource(R.plurals.settings_weekly_goal_workouts, it, it) }
            AppWheel(
                modifier = Modifier.testTag(SettingsTabTags.WeeklyGoalWheel),
                columns = listOf(
                    AppWheelColumn(
                        items = names,
                        selectedIndex = goals.indexOf(sheet.value),
                        onPick = { viewModel.onWeeklyGoalPicked(goals[it]) },
                        contentDescription = stringResource(DesignR.string.settings_weekly_goal),
                    ),
                ),
            )
            DoneButton { dismiss { viewModel.onWeeklyGoalSaved(sheet.value) } }
        }
        is SettingsTabSheet.WeightIncrease -> AppBottomSheet(
            onDismissRequest = viewModel::onSheetDismissed,
            title = stringResource(DesignR.string.settings_weight_increase),
        ) {
            val wholes = remember { SettingsTabLimits.WeightIncreaseQuarters.let { (it.first / 4)..(it.last / 4) }.toList() }
            val separator = remember { DecimalFormatSymbols.getInstance().decimalSeparator }
            val quarters = remember(separator) { listOf("00", "25", "50", "75").map { "$separator$it" } }
            AppText(text = stringResource(R.string.settings_weight_increase_description), color = Theme.contentColors.secondary)
            AppWheel(
                modifier = Modifier.testTag(SettingsTabTags.WeightIncreaseWheel),
                columns = listOf(
                    AppWheelColumn(
                        items = wholes.map { "$it" },
                        selectedIndex = wholes.indexOf(sheet.whole),
                        onPick = { viewModel.onWeightIncreasePicked(wholes[it], sheet.quarter) },
                        contentDescription = stringResource(R.string.settings_weight_increase_whole),
                    ),
                    AppWheelColumn(
                        items = quarters,
                        selectedIndex = sheet.quarter,
                        onPick = { viewModel.onWeightIncreasePicked(sheet.whole, it) },
                        contentDescription = stringResource(R.string.settings_weight_increase_quarters),
                    ),
                ),
                suffix = weightUnitAbbreviation(state.weightUnit),
            )
            DoneButton { dismiss { viewModel.onWeightIncreaseSaved(sheet.quarters) } }
        }
        SettingsTabSheet.Logout -> AppBottomSheet(
            onDismissRequest = viewModel::onSheetDismissed,
            title = stringResource(R.string.settings_logout_title),
        ) {
            AppText(text = stringResource(R.string.settings_logout_body), color = Theme.contentColors.secondary)
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { dismiss() },
                text = stringResource(R.string.settings_logout_stay),
            )
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { dismiss(onLogOutConfirmed) },
                text = stringResource(R.string.settings_logout),
            )
        }
    }
}

// A tap picks the choice and closes the sheet
@Composable
private fun <T> AppBottomSheetScope.ChoiceGroup(
    choices: List<T>,
    selected: T,
    name: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    AppListGroup {
        choices.forEach { choice ->
            row {
                AppListItem(
                    title = name(choice),
                    trailing = { AppCheck(checked = choice == selected) },
                    onClick = { dismiss { onPick(choice) } },
                )
            }
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    AppButton(modifier = Modifier.fillMaxWidth(), onClick = onClick, text = stringResource(DesignR.string.done))
}
