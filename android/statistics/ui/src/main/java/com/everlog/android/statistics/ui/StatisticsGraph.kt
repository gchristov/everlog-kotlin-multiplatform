package com.everlog.android.statistics.ui

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the statistics screens get from the app's graph, through appGraph<StatisticsGraph>()
@ContributesTo(AppScope::class)
interface StatisticsGraph {
    val userSettings: UserSettings

    val statisticsAnalytics: StatisticsAnalytics

    val navigator: Navigator
}
