package com.everlog.android.home.data

import com.everlog.android.home.data.model.HomeNotification

// What tapping Home's banner does
sealed interface HomeNotificationTap {
    // The banner needs a newer version: to the Play Store, and the banner stays
    data object UpdateApp : HomeNotificationTap

    data class OpenUrl(val url: String) : HomeNotificationTap

    // An in-app place: set suggestions, plans, settings or the exercises
    data class Open(val action: HomeNotification.ActionType) : HomeNotificationTap

    // A notice with nothing to open, e.g. maintenance
    data object Nothing : HomeNotificationTap
}

fun HomeNotification.tap(appVersionCode: Int): HomeNotificationTap {
    if (appUpdateRequired(appVersionCode)) return HomeNotificationTap.UpdateApp
    if (hasSupportedUrl()) return HomeNotificationTap.OpenUrl(actionUrl!!.trim())
    return when (val action = getAction()) {
        HomeNotification.ActionType.MUSCLE_GOALS,
        HomeNotification.ActionType.PLANS,
        HomeNotification.ActionType.SETTINGS,
        HomeNotification.ActionType.EXERCISES -> HomeNotificationTap.Open(action)
        HomeNotification.ActionType.NONE, HomeNotification.ActionType.MAINTENANCE, null -> HomeNotificationTap.Nothing
    }
}
