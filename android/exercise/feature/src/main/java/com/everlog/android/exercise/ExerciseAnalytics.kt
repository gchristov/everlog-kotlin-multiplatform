package com.everlog.android.exercise

interface ExerciseAnalytics {
    fun exerciseCreated()
    fun exerciseModified()
    fun exerciseDeleted()
}

// The names analytics already has for the exercise screens
object ExerciseScreenNames {
    const val CREATE = "screen_create_exercise"

    // Named apart from the old screen's, so its views tell apart in analytics
    const val INFO_V2 = "screen_exercise_info_v2"

    // The exercise page and its tabs (Info logs INFO_V2)
    const val DETAILS_V2 = "screen_exercise_details_v2"
    const val STATISTICS_V2 = "screen_exercise_statistics_v2"
    const val HISTORY_V2 = "screen_exercise_history_v2"
}
