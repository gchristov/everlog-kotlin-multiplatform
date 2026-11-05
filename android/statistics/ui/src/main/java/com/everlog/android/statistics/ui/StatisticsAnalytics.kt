package com.everlog.android.statistics.ui

import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.statistics.data.StatsPeriod

interface StatisticsAnalytics {
    fun statisticsRangeModified(range: RangeType)

    fun statisticsPeriodPicked(period: StatsPeriod)
}
