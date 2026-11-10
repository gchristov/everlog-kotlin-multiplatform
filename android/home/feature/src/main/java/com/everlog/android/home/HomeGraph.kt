package com.everlog.android.home

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.home.data.HomeNotifications
import com.everlog.android.home.data.workouts.LoadTemplateHistoryUseCase
import com.everlog.android.home.data.workouts.LoadWorkoutsTabListsUseCase
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.ui.PlanAnalytics
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What Home's tabs get from the app's graph, through appGraph<HomeGraph>()
@ContributesTo(AppScope::class)
interface HomeGraph {
    val userSettings: UserSettings

    val userSession: UserSession

    val planProgress: PlanProgress

    val homeNotifications: HomeNotifications

    val homeConfig: HomeConfig

    val homeAnalytics: HomeAnalytics

    val planAnalytics: PlanAnalytics

    val navigator: Navigator

    val workoutRepository: WorkoutRepository

    val loadWorkoutsTabLists: LoadWorkoutsTabListsUseCase

    val loadTemplateHistory: LoadTemplateHistoryUseCase
}
