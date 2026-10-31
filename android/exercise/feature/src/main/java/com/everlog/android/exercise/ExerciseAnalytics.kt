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
    const val STATISTICS = "screen_exercise_statistics"
    const val HISTORY = "screen_exercise_history"
}

// The feedback forms the exercise screens link to
enum class ExerciseFeedbackForm {
    PickerMenu,
    PickerNoResults,
    Statistics,
}
