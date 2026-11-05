package com.everlog.android.home.weektab

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.home.data.week.WeekStats
import org.threeten.bp.LocalDate

data class WeekTabState(
    val today: LocalDate,
    val stats: WeekStats,
    val weeklyGoal: Int,
    val weightUnit: WeightUnit,
    // Until the week's workouts first load; the days show already
    val loading: Boolean = true,
    val banner: WeekTabBanner? = null,
) {
    val empty: Boolean get() = !loading && stats.workouts == 0

    val workoutsLeft: Int get() = (weeklyGoal - stats.workouts).coerceAtLeast(0)

    // Of the weekly goal, from 0 to 1
    val goalProgress: Float get() = if (weeklyGoal <= 0) 1f else (stats.workouts.toFloat() / weeklyGoal).coerceAtMost(1f)
}

data class WeekTabBanner(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val updateRequired: Boolean,
)
