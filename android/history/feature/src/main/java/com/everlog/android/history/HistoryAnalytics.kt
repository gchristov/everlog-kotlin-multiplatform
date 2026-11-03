package com.everlog.android.history

import com.everlog.android.common.data.model.workout.RangeType

interface HistoryAnalytics {
    fun statisticsRangeModified(range: RangeType)
}
