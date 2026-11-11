package com.everlog.android.settings.ui.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBarHeader
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppSwitch
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppGroupedList
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.list.items
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.R as KotlinR
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.FormatUtils
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.settings.ui.R
import com.everlog.android.settings.ui.SetSuggestionText
import java.util.Locale
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle

/**
 * @param describeSetSuggestion How Set suggestions names a goal, or null for a goal it doesn't offer, which keeps its
 * old name.
 */
@Composable
fun SettingsTabScreen(
    viewModel: SettingsTabViewModel,
    actions: SettingsTabActions,
    describeSetSuggestion: (MuscleGoal) -> SetSuggestionText?,
    // Room for Home's button over the end of the list
    bottomPadding: Dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsTabContent(
        state = state,
        actions = actions,
        describeSetSuggestion = describeSetSuggestion,
        bottomPadding = bottomPadding,
        onFirstDayOfWeek = viewModel::onFirstDayOfWeekTapped,
        onWeightUnit = viewModel::onWeightUnitTapped,
        onWeeklyGoal = viewModel::onWeeklyGoalTapped,
        onWeightIncrease = viewModel::onWeightIncreaseTapped,
        onKeepScreenOn = viewModel::onKeepScreenOnTapped,
        onLogOut = viewModel::onLogOutTapped,
    )
    SettingsTabSheets(
        state = state,
        viewModel = viewModel,
        onLogOutConfirmed = actions.logOut,
    )
}

@Composable
internal fun SettingsTabContent(
    state: SettingsTabState,
    actions: SettingsTabActions,
    describeSetSuggestion: (MuscleGoal) -> SetSuggestionText?,
    bottomPadding: Dp,
    onFirstDayOfWeek: () -> Unit,
    onWeightUnit: () -> Unit,
    onWeeklyGoal: () -> Unit,
    onWeightIncrease: () -> Unit,
    onKeepScreenOn: () -> Unit,
    onLogOut: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AppBarHeader(title = stringResource(DesignR.string.settings_title))
        AppGroupedList(
            modifier = Modifier.testTag(SettingsTabTags.List),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            state.account?.let { account ->
                item(key = "account") { SettingsAccountHeader(account, Modifier.padding(bottom = Theme.spacing.large)) }
            }
            group(key = "general", header = { stringResource(R.string.settings_general) }) {
                val rows = listOf<SettingsRow>(
                    SettingsRow("firstDayOfWeek") {
                        AppListItem(
                            title = stringResource(R.string.settings_first_week_day),
                            trailing = { SettingsValue(state.firstDayOfWeek.displayName()) },
                            onClick = onFirstDayOfWeek,
                        )
                    },
                    SettingsRow("weightUnit") {
                        AppListItem(
                            title = stringResource(R.string.settings_unit_weight),
                            trailing = { SettingsValue(weightUnitName(state.weightUnit)) },
                            onClick = onWeightUnit,
                        )
                    },
                )
                items(rows, key = { it.key }) { it.content() }
            }
            group(key = "workouts", header = { stringResource(DesignR.string.settings_workouts) }) {
                val rows = listOf<SettingsRow>(
                    SettingsRow("weeklyGoal") {
                        AppListItem(
                            title = stringResource(DesignR.string.settings_weekly_goal),
                            trailing = { SettingsValue("${state.weeklyGoal}") },
                            onClick = onWeeklyGoal,
                        )
                    },
                    SettingsRow("setSuggestions") {
                        // Goals Set suggestions doesn't offer, which older versions could set, keep their old names
                        val described = describeSetSuggestion(state.setSuggestion)
                        AppListItem(
                            title = stringResource(DesignR.string.set_suggestions_title),
                            subtitle = described?.subtitle ?: state.setSuggestion.valueSettingsSummary(context, false),
                            trailing = { SettingsValue(described?.value ?: state.setSuggestion.valueName(context)) },
                            onClick = actions.openSetSuggestions,
                        )
                    },
                    SettingsRow("weightIncrease") {
                        AppListItem(
                            title = stringResource(DesignR.string.settings_weight_increase),
                            subtitle = stringResource(R.string.settings_weight_increase_description),
                            trailing = { SettingsValue(formatWeightIncrease(state.weightIncrease, state.weightUnit)) },
                            onClick = onWeightIncrease,
                        )
                    },
                    SettingsRow("keepScreenOn") {
                        // The whole row is the switch, so TalkBack reads its title with on or off
                        AppListItem(
                            modifier = Modifier
                                .testTag(SettingsTabTags.KeepScreenOn)
                                .toggleable(value = state.keepScreenOn, role = Role.Switch, onValueChange = { onKeepScreenOn() }),
                            title = stringResource(R.string.settings_keep_screen_on),
                            subtitle = stringResource(R.string.settings_keep_screen_on_description),
                            trailing = { AppSwitch(checked = state.keepScreenOn) },
                        )
                    },
                )
                items(rows, key = { it.key }) { it.content() }
            }
            if (state.showDeveloper) {
                // Debug builds only, so not translated
                group(key = "developer", header = { "Developer" }) {
                    items(count = 1, key = { "designSystem" }) {
                        AppListItem(title = "Design system", onClick = actions.openDesignSystem)
                    }
                }
            }
            group(key = "contact", footer = { state.appInfo }) {
                val rows = buildList {
                    if (state.showFeedback) {
                        add(SettingsRow("feedback") { ContactRow(DesignR.string.feedback_send, DesignR.drawable.ic_feedback, actions.sendFeedback) })
                    }
                    add(SettingsRow("review") { ContactRow(R.string.settings_play_store_prompt, DesignR.drawable.ic_star_filled, actions.reviewOnPlay) })
                    add(SettingsRow("share") { ContactRow(R.string.settings_share, DesignR.drawable.ic_share, actions.share) })
                    if (state.account != null) {
                        add(SettingsRow("logOut") { ContactRow(R.string.settings_logout, R.drawable.ic_logout, onLogOut) })
                    }
                }
                items(rows, key = { it.key }) { it.content() }
            }
        }
    }
}

private class SettingsRow(val key: String, val content: @Composable () -> Unit)

@Composable
private fun SettingsValue(text: String) {
    AppText(text = text, style = Theme.typography.bodyBold, color = Theme.contentColors.action)
}

@Composable
private fun ContactRow(title: Int, icon: Int, onClick: () -> Unit) {
    AppListItem(
        title = stringResource(title),
        leading = { AppIcon(imageVector = ImageVector.vectorResource(icon), tint = Theme.contentColors.action) },
        onClick = onClick,
    )
}

@Composable
internal fun DayOfWeek.displayName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
internal fun weightUnitName(unit: WeightUnit): String =
    stringResource(if (unit == WeightUnit.KILOGRAM) KotlinR.string.settings_kilograms else KotlinR.string.settings_pounds)

internal fun formatWeightIncrease(value: Float, unit: WeightUnit): String =
    "${FormatUtils.formatSetWeight(value)} ${weightUnitAbbreviation(unit)}"

@Preview
@Composable
private fun SettingsTabContentPreview() {
    Theme {
        SettingsTabContent(
            state = SettingsTabState(
                account = SettingsAccountInfo(name = "Alex", photoUrl = null),
                firstDayOfWeek = DayOfWeek.MONDAY,
                weightUnit = WeightUnit.KILOGRAM,
                weeklyGoal = 3,
                setSuggestion = MuscleGoal.HISTORY,
                weightIncrease = 2.5f,
                keepScreenOn = true,
                showFeedback = true,
                showDeveloper = true,
                appInfo = "everlog 2.24.0 (2240000)",
            ),
            actions = SettingsTabActions({}, {}, {}, {}, {}, {}),
            describeSetSuggestion = { SetSuggestionText("From last time", "Fill in the reps and weight you did last time") },
            bottomPadding = 120.dp,
            onFirstDayOfWeek = {},
            onWeightUnit = {},
            onWeeklyGoal = {},
            onWeightIncrease = {},
            onKeepScreenOn = {},
            onLogOut = {},
        )
    }
}
