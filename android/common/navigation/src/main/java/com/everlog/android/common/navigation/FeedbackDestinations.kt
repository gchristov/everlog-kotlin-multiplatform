package com.everlog.android.common.navigation

// Opens the feedback form for [source]. Remote Config may have none, see Navigator.canOpen.
data class FeedbackForm(val source: FeedbackSource) : Destination

/** Where a feedback form was opened from. [id] is what analytics and the form's `{source}` get. */
enum class FeedbackSource(val id: String, val form: Form) {
    SETTINGS("settings", Form.GENERAL),
    ACTIVITY_STATISTICS("activity_statistics", Form.STATISTICS),
    EXERCISE_STATISTICS("exercise_statistics", Form.STATISTICS),
    SET_TYPES("set_types", Form.SET_TYPES),
    MUSCLE_GOAL("muscle_goal", Form.SET_SUGGESTIONS),
    SET_SUGGESTIONS("set_suggestions", Form.SET_SUGGESTIONS),
    WORKOUT_MENU("workout_menu", Form.WORKOUT),
    EXERCISE_PICKER_MENU("exercise_picker_menu", Form.EXERCISE_PICKER),
    EXERCISE_PICKER_NO_RESULTS("exercise_picker_no_results", Form.EXERCISE_PICKER);

    enum class Form { GENERAL, STATISTICS, SET_TYPES, SET_SUGGESTIONS, WORKOUT, EXERCISE_PICKER }
}
