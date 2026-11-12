package com.everlog.android.statistics.ui

import com.everlog.android.statistics.data.StatsPeriod

interface StatisticsAnalytics {
    fun statisticsPeriodPicked(period: StatsPeriod)
}
