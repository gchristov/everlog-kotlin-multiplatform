package com.everlog.android.settings.ui

// How Set suggestions names a goal, for Settings to show
data class SetSuggestionText(val value: String, val subtitle: String)

interface SettingsAnalytics {
    fun weightIncreaseModified(value: Float)
    fun weeklyGoalModified(value: Int)
    fun keepScreenOnModified(value: Boolean)
    fun weightUnitModified(unit: String)
    fun firstWeekDayModified(day: String)
}

// The names analytics already has for the settings screens
object SettingsScreenNames {
    const val HOME = "screen_settings"
    const val HOME_V2 = "screen_settings_v2"
}
