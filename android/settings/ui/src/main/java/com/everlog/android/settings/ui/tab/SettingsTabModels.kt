package com.everlog.android.settings.ui.tab

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.MuscleGoal
import org.threeten.bp.DayOfWeek

data class SettingsTabState(
    // Null for a guest, who has no account to show or log out of
    val account: SettingsAccountInfo?,
    val firstDayOfWeek: DayOfWeek,
    val weightUnit: WeightUnit,
    val weeklyGoal: Int,
    val setSuggestion: MuscleGoal,
    // Remote Config's set_suggestions_v2: the row says Set suggestions rather than Muscle goal
    val setSuggestionsV2: Boolean,
    val weightIncrease: Float,
    val keepScreenOn: Boolean,
    val showFeedback: Boolean,
    val showDeveloper: Boolean,
    val appInfo: String,
    val sheet: SettingsTabSheet? = null,
)

data class SettingsAccountInfo(val name: String?, val photoUrl: String?)

sealed interface SettingsTabSheet {
    data object FirstDayOfWeek : SettingsTabSheet
    data object WeightUnit : SettingsTabSheet
    data class WeeklyGoal(val value: Int) : SettingsTabSheet
    // In quarters, as its wheels pick it: whole and quarter
    data class WeightIncrease(val quarters: Int) : SettingsTabSheet {
        val whole get() = quarters / 4
        val quarter get() = quarters % 4
    }
    data object Logout : SettingsTabSheet
}

// What the tab opens or asks of its host; the screen calls them, the fragment carries them out
class SettingsTabActions(
    val openSetSuggestions: () -> Unit,
    val sendFeedback: () -> Unit,
    val reviewOnPlay: () -> Unit,
    val share: () -> Unit,
    val openDesignSystem: () -> Unit,
    val logOut: () -> Unit,
)

object SettingsTabLimits {
    val WeeklyGoals = 1..7
    // 1 to 20.75, a quarter at a time, as the old picker allowed
    val WeightIncreaseQuarters = 4..83
    val FirstDaysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
}
