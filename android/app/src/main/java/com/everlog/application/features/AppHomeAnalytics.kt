package com.everlog.application.features

import com.everlog.android.home.HomeAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHomeAnalytics : HomeAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun planDayCompleted() = analytics.planDayComplete()

    override fun planCompleted() = analytics.planCompleted()

    override fun notificationShown(title: String?) = analytics.notificationHomeShown(title)

    override fun notificationDismissed(title: String?) = analytics.notificationHomeDismissed(title)

    override fun notificationTapped(title: String?) = analytics.notificationHomeTapped(title)

    override fun workoutsCardShown(kind: String) = analytics.workoutsCardShown(kind)

    override fun workoutsTabStarted(from: String, card: String?, suggested: Boolean) = analytics.workoutsTabStarted(from, card, suggested)
}
