package com.everlog.config

import java.io.Serializable
import java.net.URLEncoder

/**
 * The `feedback_forms` Remote Config parameter: a form URL for each place in the app. A URL can hold
 * `{appVersion}` and `{source}`, which are filled in when the form opens, so a Google Forms prefill
 * link can say where the answer came from.
 */
data class FeedbackForms(
        var general: String? = null,
        var statistics: String? = null,
        var setTypes: String? = null,
        var setSuggestions: String? = null,
        var workout: String? = null,
        var exercisePicker: String? = null
) : Serializable {

    /** The form to open from [source], or null when there's none and its entry point is hidden. */
    fun url(source: FeedbackSource, appVersion: String): String? {
        val url = formFor(source.form)?.trim()
        if (url.isNullOrEmpty()) {
            return null
        }
        return url
                .replace(APP_VERSION, encode(appVersion))
                .replace(SOURCE, encode(source.id))
    }

    private fun formFor(form: FeedbackSource.Form) = when (form) {
        FeedbackSource.Form.GENERAL -> general
        FeedbackSource.Form.STATISTICS -> statistics
        FeedbackSource.Form.SET_TYPES -> setTypes
        FeedbackSource.Form.SET_SUGGESTIONS -> setSuggestions
        FeedbackSource.Form.WORKOUT -> workout
        FeedbackSource.Form.EXERCISE_PICKER -> exercisePicker
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val APP_VERSION = "{appVersion}"
        const val SOURCE = "{source}"
    }
}

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
