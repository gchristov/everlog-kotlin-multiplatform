package com.everlog.ui.activities.onboarding

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager

/**
 * The app settings some answers set, written as soon as the question is answered: the units, and
 * days a week as the weekly workouts goal. A change logs the same event as in Settings.
 */
interface OnboardingSettings {
    fun setWeightUnit(unit: SettingsManager.WeightUnit)

    fun setWeeklyWorkoutsGoal(count: Int)
}

class RealOnboardingSettings(context: Context) : OnboardingSettings {
    private val context = context.applicationContext

    override fun setWeightUnit(unit: SettingsManager.WeightUnit) {
        if (SettingsManager.manager.weightUnit() == unit) return
        SettingsManager.manager.setWeightUnit(unit)
        notifyChanged()
        AnalyticsManager.manager.settingsWeightUnitModified(unit.name)
    }

    override fun setWeeklyWorkoutsGoal(count: Int) {
        if (SettingsManager.manager.weeklyWorkoutsGoal() == count) return
        SettingsManager.manager.setWeeklyWorkoutsGoal(count)
        notifyChanged()
        AnalyticsManager.manager.settingsWeeklyGoalModified(count)
    }

    // As the Settings screen does, so open screens show the new value
    private fun notifyChanged() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ELConstants.BROADCAST_PREFERENCES_CHANGED))
    }
}
