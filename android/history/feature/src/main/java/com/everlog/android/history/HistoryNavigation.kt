package com.everlog.android.history

import android.content.Context
import com.everlog.android.common.data.model.workout.ELWorkout

// The other screens the Activity tab opens, through the app's Navigator
interface HistoryNavigation {
    fun openWorkoutDetails(context: Context, workout: ELWorkout)

    // Whether Remote Config has a feedback form for the statistics, which the screen offers when it does
    fun hasStatisticsFeedback(): Boolean

    fun openStatisticsFeedback(context: Context)
}
