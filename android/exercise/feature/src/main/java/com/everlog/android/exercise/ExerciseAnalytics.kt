package com.everlog.android.exercise

import com.everlog.android.common.data.model.workout.RangeType

interface ExerciseAnalytics {
    fun exerciseCreated()
    fun exerciseModified()
    fun exerciseDeleted()
    fun statisticsRangeModified(range: RangeType)
}

// The names analytics already has for the exercise screens
object ExerciseScreenNames {
    const val DETAILS = "screen_exercise_details"
    const val CREATE = "screen_create_exercise"
    const val INFO = "screen_exercise_info"

    // The Compose Info tab, so its views tell apart from the old one's
    const val INFO_V2 = "screen_exercise_info_v2"
    const val STATISTICS = "screen_exercise_statistics"
    const val HISTORY = "screen_exercise_history"
}
