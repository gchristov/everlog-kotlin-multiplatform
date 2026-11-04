package com.everlog.android.plan

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.ui.PlanAnalytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the plan screens get from the app's graph, through appGraph<PlanGraph>()
@ContributesTo(AppScope::class)
interface PlanGraph {
    val planProgress: PlanProgress

    val userSession: UserSession

    val planAnalytics: PlanAnalytics

    val navigator: Navigator
}
