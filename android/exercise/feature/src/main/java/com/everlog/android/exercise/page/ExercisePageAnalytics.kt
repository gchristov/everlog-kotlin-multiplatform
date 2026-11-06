package com.everlog.android.exercise.page

import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange

interface ExercisePageAnalytics {
    // Logged as the page shows, before its other events, so they're credited to it
    fun screenViewed()

    fun tabViewed(tab: ExercisePageTab)

    fun rangePicked(range: ExerciseStatsRange)

    fun exerciseDeleted()
}
