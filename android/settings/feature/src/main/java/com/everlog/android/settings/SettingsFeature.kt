package com.everlog.android.settings

// How Set suggestions names a goal, for Settings to show
data class SetSuggestionText(val value: String, val subtitle: String)

interface SettingsAnalytics {
    fun weightIncreaseModified(value: Float)
    fun weeklyGoalModified(value: Int)
    fun keepScreenOnModified(value: Boolean)
    fun weightUnitModified(unit: String)
    fun firstWeekDayModified(day: String)
    fun newsletterGranted()
    fun newsletterDenied()
}

// What the settings screens are opened with. The app reads the same names.
object SettingsExtras {
    const val INTEGRATION = "EXTRA_INTEGRATION"
}

// The names analytics already has for the settings screens
object SettingsScreenNames {
    const val HOME = "screen_settings"
    const val INTEGRATION = "screen_integration"
}
