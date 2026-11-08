package com.everlog.application.features

import com.everlog.android.settings.SettingsAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppSettingsAnalytics : SettingsAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun weightIncreaseModified(value: Float) = analytics.settingsWeightModified(value)

    override fun weeklyGoalModified(value: Int) = analytics.settingsWeeklyGoalModified(value)

    override fun keepScreenOnModified(value: Boolean) = analytics.settingsKeepScreenOnModified(value)

    override fun weightUnitModified(unit: String) = analytics.settingsWeightUnitModified(unit)

    override fun firstWeekDayModified(day: String) = analytics.settingsFirstWeekDayModified(day)
}
