package com.everlog.application.features

import com.everlog.android.exercise.picker.ExercisePickerAnalytics
import com.everlog.android.exercise.picker.PickerMode
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager

class AppExercisePickerAnalytics : ExercisePickerAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed(mode: PickerMode) = analytics.screenName(
        if (mode == PickerMode.Pick) AnalyticsConstants.SCREEN_EXERCISE_PICKER_V2 else AnalyticsConstants.SCREEN_EXERCISES_V2,
    )

    override fun filterAdded(filter: String, value: String, source: String) = analytics.exercisePickerFilterAdded(filter, value, source)

    override fun filterRemoved(filter: String) = analytics.exercisePickerFilterRemoved(filter)

    override fun filterOperatorChanged(filter: String, not: Boolean) = analytics.exercisePickerFilterOperatorChanged(filter, not)

    override fun searched(words: Int, results: Int) = analytics.exercisePickerSearched(words, results)

    override fun noResults(text: String?, filters: Int) = analytics.exercisePickerNoResults(text, filters)

    override fun exercisePicked(section: String, position: Int) = analytics.exercisePickerExercisePicked(section, position)

    override fun createTapped(source: String) = analytics.exercisePickerCreateTapped(source)

    override fun added(count: Int, setType: String, filters: Int, hadText: Boolean) =
        analytics.exercisePickerAdded(count, setType, filters, hadText)
}
