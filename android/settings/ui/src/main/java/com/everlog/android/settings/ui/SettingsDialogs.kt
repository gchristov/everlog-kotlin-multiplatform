package com.everlog.android.settings.ui

import android.content.Context
import rx.Observable

// The app's pickers for Settings' values
interface SettingsDialogs {
    fun showWeightIncreasePrompt(context: Context, value: Float): Observable<Float>

    fun showWeeklyGoalPrompt(context: Context, value: Int): Observable<Int>
}
