package com.everlog.application.features

import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.ui.StatisticsAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppStatisticsAnalytics : StatisticsAnalytics {
    override fun statisticsRangeModified(range: RangeType) = AnalyticsManager.manager.statisticsRangeModified(range.name)

    override fun statisticsPeriodPicked(period: StatsPeriod) = AnalyticsManager.manager.statisticsRangeModified(
        when (period) {
            StatsPeriod.Week -> "WEEK"
            StatsPeriod.Month -> "MONTH"
            StatsPeriod.Last12Months -> "LAST_12_MONTHS"
        },
    )
}
