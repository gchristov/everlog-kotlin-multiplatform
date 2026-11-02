package com.everlog.application.features

import com.everlog.application.ELApplication
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.onboarding.OnboardingSettings
import com.everlog.managers.analytics.AnalyticsManager

class AppOnboardingSettings(context: Context) : OnboardingSettings {
    private val context = context.applicationContext

    override fun setWeightUnit(unit: WeightUnit) {
        if (ELApplication.graph.userSettings.weightUnit() == unit) return
        ELApplication.graph.userSettings.setWeightUnit(unit)
        notifyChanged()
        AnalyticsManager.manager.settingsWeightUnitModified(unit.name)
    }

    override fun setWeeklyWorkoutsGoal(count: Int) {
        if (ELApplication.graph.userSettings.weeklyWorkoutsGoal() == count) return
        ELApplication.graph.userSettings.setWeeklyWorkoutsGoal(count)
        notifyChanged()
        AnalyticsManager.manager.settingsWeeklyGoalModified(count)
    }

    // As the Settings screen does, so open screens show the new value
    private fun notifyChanged() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
    }
}
