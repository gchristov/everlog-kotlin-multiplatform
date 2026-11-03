package com.everlog.application.features

import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.history.HistoryAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHistoryAnalytics : HistoryAnalytics {
    override fun statisticsRangeModified(range: RangeType) = AnalyticsManager.manager.statisticsRangeModified(range)
}
