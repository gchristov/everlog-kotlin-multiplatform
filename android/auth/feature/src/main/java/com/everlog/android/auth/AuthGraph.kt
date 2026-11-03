package com.everlog.android.auth

import com.everlog.android.common.navigation.Navigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the login screens get from the app's graph, through appGraph<AuthGraph>()
@ContributesTo(AppScope::class)
interface AuthGraph {
    val authService: AuthService

    val navigator: Navigator

    val authAppState: AuthAppState
}
