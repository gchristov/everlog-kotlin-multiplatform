package com.everlog.android.settings.ui

import android.content.Context
import com.everlog.android.settings.data.MuscleGoal

// What Settings shows that the app knows: other screens' names and the app's version
interface SettingsConfig {
    // How Set suggestions names [goal], or null for a goal it doesn't offer
    fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText?

    // The app's version and build, for the bottom of Settings
    fun appInfo(): String
}
