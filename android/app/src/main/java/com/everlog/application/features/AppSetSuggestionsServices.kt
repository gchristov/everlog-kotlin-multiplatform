package com.everlog.application.features

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.workout.WorkoutScreenNames
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import com.everlog.managers.analytics.AnalyticsManager

class AppSetSuggestionsSettings(context: Context) : SetSuggestionsSettings {
    private val context = context.applicationContext

    override fun muscleGoal(): MuscleGoal = SettingsManager.manager.muscleGoal()

    override fun setMuscleGoal(goal: MuscleGoal) {
        SettingsManager.manager.setMuscleGoal(goal)
        // As the Settings screen does, so open screens show the new value
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
        AnalyticsManager.manager.settingsMuscleGoalModified(goal)
    }

    override fun weightUnit(): WeightUnit = SettingsManager.manager.weightUnit()
}

class AppSetSuggestionsAnalytics : SetSuggestionsAnalytics {
    override fun screenViewed() = AnalyticsManager.manager.screenName(WorkoutScreenNames.SET_SUGGESTIONS)

    override fun howOpened() = AnalyticsManager.manager.setSuggestionsHowOpened()
}
