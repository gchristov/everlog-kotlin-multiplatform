package com.everlog.ui.activities.exercisepicker

import com.everlog.data.model.exercise.picker.StoredFilterToken
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.PreferencesManager
import com.google.gson.Gson
import timber.log.Timber

/**
 * The picker's analytics events, so the view-model can be tested without Firebase. Filters and
 * sections are named for BigQuery: muscle, equipment, created_by…; most_used, recently_done,
 * movement, yours. The screen view (screen_exercises_v2 or screen_exercise_picker_v2) says the mode.
 */
internal interface ExercisePickerAnalytics {
    fun screenViewed(mode: PickerMode)

    // source: chip, menu, token, typed or recent
    fun filterAdded(filter: String, value: String, source: String)

    fun filterRemoved(filter: String)

    fun filterOperatorChanged(filter: String, not: Boolean)

    // Once typing settles, never the text
    fun searched(words: Int, results: Int)

    // The search text, to find what the library lacks
    fun noResults(text: String?, filters: Int)

    fun exercisePicked(section: String, position: Int)

    // source: menu (the app bar's ⋮) or search
    fun createTapped(source: String)

    fun added(count: Int, setType: String, filters: Int, hadText: Boolean)
}

internal class RealExercisePickerAnalytics : ExercisePickerAnalytics {
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

// The picker's recent searches, kept on the device
internal interface ExercisePickerRecentSearches {
    fun load(): List<List<FilterToken>>

    fun save(searches: List<List<FilterToken>>)
}

internal class RealExercisePickerRecentSearches : PreferencesManager(), ExercisePickerRecentSearches {

    private enum class PreferenceKeys {
        EXERCISE_PICKER_RECENT_SEARCHES
    }

    override fun load(): List<List<FilterToken>> {
        val json = getPreference(PreferenceKeys.EXERCISE_PICKER_RECENT_SEARCHES.name, "")
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            // Arrays rather than a TypeToken, whose generic type R8 can strip in release builds
            Gson().fromJson(json, Array<Array<StoredFilterToken>>::class.java).orEmpty().mapNotNull { search ->
                search.mapNotNull { stored ->
                    val key = FilterKey.entries.find { it.name == stored.key } ?: return@mapNotNull null
                    val values = stored.values.orEmpty().ifEmpty { return@mapNotNull null }
                    FilterToken(key = key, values = values, not = stored.not)
                }.ifEmpty { null }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Couldn't read the recent searches")
            emptyList()
        }
    }

    override fun save(searches: List<List<FilterToken>>) {
        val stored = searches.map { search -> search.map { StoredFilterToken(it.key.name, it.values, it.not) } }
        savePreference(Gson().toJson(stored), PreferenceKeys.EXERCISE_PICKER_RECENT_SEARCHES.name)
    }

    private companion object {
        const val TAG = "ExercisePickerRecent"
    }
}
