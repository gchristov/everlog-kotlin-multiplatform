package com.everlog.android.history.ui

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the history screens get from the app's graph, through appGraph<HistoryGraph>()
@ContributesTo(AppScope::class)
interface HistoryGraph {
    val userSettings: UserSettings

    val navigator: Navigator

    val workoutRepository: WorkoutRepository
}
