package com.everlog.android.pro

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the Pro screen gets from the app's graph, through appGraph<ProGraph>()
@ContributesTo(AppScope::class)
interface ProGraph {
    val proPurchases: ProPurchases

    val proAnalytics: ProAnalytics

    val proNavigation: ProNavigation
}
