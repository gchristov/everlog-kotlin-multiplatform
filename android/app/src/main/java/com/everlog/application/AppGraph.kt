package com.everlog.application

import android.content.Context
import com.everlog.android.auth.data.UserSession
import com.everlog.android.exercise.data.ExerciseImageStorage
import com.everlog.android.exercise.data.ExerciseLookup
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.settings.data.RealUserSettings
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

// What the app hands to the features, one of each for the app's lifetime. The modules contribute their bindings.
@DependencyGraph(AppScope::class)
interface AppGraph {
    val userSettings: UserSettings

    // The same instance, for the session flags only the class has
    val realUserSettings: RealUserSettings

    val userSession: UserSession

    val planProgress: PlanProgress

    val exerciseImageStorage: ExerciseImageStorage

    val exerciseLookup: ExerciseLookup

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Context): AppGraph
    }
}
