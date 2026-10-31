package com.everlog.android.history

import android.content.Context
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType

/**
 * What the history feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [HistoryFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface HistoryFeatureDependencies {
    fun openWorkoutDetails(context: Context, workout: ELWorkout)

    // Whether Remote Config has a feedback form for the statistics, which the screen offers when it does
    fun hasStatisticsFeedback(): Boolean

    fun openStatisticsFeedback(context: Context)

    fun statisticsRangeModified(range: RangeType)
}

object HistoryFeature {
    lateinit var dependencies: HistoryFeatureDependencies
}

// The names analytics already has for the history screens
object HistoryScreenNames {
    const val ACTIVITY = "screen_activity"
    const val STATISTICS = "screen_statistics"
    const val HISTORY = "screen_history"
}
