package com.everlog.application.features

import com.everlog.android.plan.PlanAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppPlanAnalytics : PlanAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun created() = analytics.planCreated()

    override fun modified() = analytics.planModified()

    override fun deleted() = analytics.planDeleted()

    override fun started() = analytics.planStarted()

    override fun stopped() = analytics.planStopped()

    override fun weekAdded() = analytics.planWeekAdded()

    override fun weekDeleted() = analytics.planWeekDeleted()

    override fun daySetToTemplate() = analytics.planWeekDaySetRoutine()

    override fun daySetToRest() = analytics.planWeekDaySetRest()
}
