package com.everlog.android.home

import com.everlog.android.home.data.model.HomeNotification

interface HomeAnalytics {
    fun planDayCompleted()

    fun planCompleted()

    fun notificationShown(title: String?)

    fun notificationDismissed(title: String?)

    fun notificationTapped(title: String?)

    // The Workouts tab's card on top showed, by its kind (plan, suggested, start_here, get_started)
    fun workoutsCardShown(kind: String)

    /**
     * A workout started from the Workouts tab.
     *
     * @param from Where on the tab: the card on top or a template's card.
     * @param card The kind of card on top then, null without one.
     * @param suggested Whether it was the template the card suggested.
     */
    fun workoutsTabStarted(from: String, card: String?, suggested: Boolean)
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
    const val WORKOUTS_V2 = "screen_workouts_v2"
    const val ACTIVITY = "screen_activity"
}
