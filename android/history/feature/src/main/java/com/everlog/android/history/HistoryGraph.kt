package com.everlog.android.history

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the Activity tab gets from the app's graph, through appGraph<HistoryGraph>()
@ContributesTo(AppScope::class)
interface HistoryGraph {
    val userSettings: UserSettings

    val historyAnalytics: HistoryAnalytics

    val navigator: Navigator
}
