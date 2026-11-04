package com.everlog.android.statistics.ui

import com.everlog.android.common.data.model.workout.RangeType

interface StatisticsAnalytics {
    fun statisticsRangeModified(range: RangeType)
}
