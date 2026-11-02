package com.everlog.application

import com.everlog.android.auth.data.UserSession
import com.everlog.android.exercise.data.ExerciseImageStorage
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.settings.data.RealUserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

// What the app hands to the features, one of each for the app's lifetime. The modules contribute their bindings.
@DependencyGraph(AppScope::class)
interface AppGraph {
    // The class rather than UserSettings, as the app also reads the session flags only it has
    val userSettings: RealUserSettings

    val userSession: UserSession

    val planProgress: PlanProgress

    val exerciseImageStorage: ExerciseImageStorage
}
