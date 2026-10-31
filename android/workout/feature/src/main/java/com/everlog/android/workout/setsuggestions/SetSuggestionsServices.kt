package com.everlog.android.workout.setsuggestions

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.SettingsManager.MuscleGoal

/**
 * The setting the screen changes, stored as the muscle goal. A change logs the same event as the old screen.
 */
interface SetSuggestionsSettings {
    fun muscleGoal(): MuscleGoal

    fun setMuscleGoal(goal: MuscleGoal)

    fun weightUnit(): WeightUnit
}

interface SetSuggestionsAnalytics {
    fun screenViewed()

    fun howOpened()
}
