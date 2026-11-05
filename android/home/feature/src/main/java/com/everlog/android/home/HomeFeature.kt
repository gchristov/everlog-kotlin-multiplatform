package com.everlog.android.home

import com.everlog.android.home.data.model.HomeNotification

interface HomeAnalytics {
    fun planDayCompleted()

    fun planCompleted()

    fun notificationShown(title: String?)

    fun notificationDismissed(title: String?)

    fun notificationTapped(title: String?)
}

interface HomeConfig {
    // The banner Remote Config sets for Home, if any
    fun notification(): HomeNotification?

    val appVersionCode: Int

    // The Activity tab's statistics page in Compose
    fun statisticsV2(): Boolean
}

// The names analytics already has for Home's tabs
object HomeScreenNames {
    const val WEEK = "screen_week"
    const val ACTIVITY = "screen_activity"
}
