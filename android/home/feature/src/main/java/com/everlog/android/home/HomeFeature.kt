package com.everlog.android.home

import com.everlog.android.home.data.model.HomeNotification

interface HomeAnalytics {
    fun planDayCompleted()

    fun planCompleted()

    fun notificationShown(title: String?)

    fun notificationDismissed(title: String?)

    fun notificationTapped(title: String?)

    // Home's Start workout button, or the empty week's own
    fun startWorkoutTapped(fromEmptyWeek: Boolean)

    // The workout left going, opened again as Home came back
    fun workoutResumedOnLaunch(setsCompleted: Int)

    // The prompt to resume or discard the workout left going
    fun ongoingWorkoutPromptShown(setsCompleted: Int)

    fun ongoingWorkoutPromptCancelled()

    fun ongoingWorkoutDiscarded()
}

interface HomeConfig {
    // The banner Remote Config sets for Home, if any
    fun notification(): HomeNotification?

    val appVersionCode: Int

    // The Activity tab's statistics page in Compose
    fun statisticsV2(): Boolean

    // The Activity tab's history page in Compose
    fun historyV2(): Boolean
}

// The names analytics already has for Home's tabs
object HomeScreenNames {
    const val WEEK = "screen_week"

    // The Compose tab, so its views tell apart from the old one's
    const val WEEK_V2 = "screen_week_v2"
    const val ACTIVITY = "screen_activity"
}
