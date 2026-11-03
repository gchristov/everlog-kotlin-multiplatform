package com.everlog.android.onboarding

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.onboarding.data.BuildStarterRoutinesUseCase
import com.everlog.android.onboarding.data.SaveStarterRoutinesUseCase
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the onboarding gets from the app's graph, through appGraph<OnboardingGraph>()
@ContributesTo(AppScope::class)
interface OnboardingGraph {
    val userSettings: UserSettings

    val buildStarterRoutines: BuildStarterRoutinesUseCase

    val saveStarterRoutines: SaveStarterRoutinesUseCase

    val onboardingSettings: OnboardingSettings

    val onboardingAnalytics: OnboardingAnalytics

    val navigator: Navigator
}
