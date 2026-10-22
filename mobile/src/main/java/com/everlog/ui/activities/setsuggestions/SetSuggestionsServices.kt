package com.everlog.ui.activities.setsuggestions

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.managers.preferences.SettingsManager.WeightUnit

/**
 * The setting the screen changes, stored as the muscle goal. A change logs the same event as the old screen.
 */
interface SetSuggestionsSettings {
    fun muscleGoal(): MuscleGoal

    fun setMuscleGoal(goal: MuscleGoal)

    fun weightUnit(): WeightUnit
}

class RealSetSuggestionsSettings(context: Context) : SetSuggestionsSettings {
    private val context = context.applicationContext

    override fun muscleGoal(): MuscleGoal = SettingsManager.manager.muscleGoal()

    override fun setMuscleGoal(goal: MuscleGoal) {
        SettingsManager.manager.setMuscleGoal(goal)
        // As the Settings screen does, so open screens show the new value
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ELConstants.BROADCAST_PREFERENCES_CHANGED))
        AnalyticsManager.manager.settingsMuscleGoalModified(goal)
    }

    override fun weightUnit(): WeightUnit = SettingsManager.manager.weightUnit()
}

interface SetSuggestionsAnalytics {
    fun screenViewed()

    fun howOpened()
}

class RealSetSuggestionsAnalytics : SetSuggestionsAnalytics {
    override fun screenViewed() = AnalyticsManager.manager.screenName(AnalyticsConstants.SCREEN_SET_SUGGESTIONS)

    override fun howOpened() = AnalyticsManager.manager.setSuggestionsHowOpened()
}
