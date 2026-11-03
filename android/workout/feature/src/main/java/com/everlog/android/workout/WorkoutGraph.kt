package com.everlog.android.workout

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.template.data.session.OngoingPlanRepository
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.prefill.LoadPrefillSourcesUseCase
import com.everlog.android.workout.details.WorkoutDetailsAnalytics
import com.everlog.android.workout.log.WorkoutLogAlerts
import com.everlog.android.workout.log.WorkoutLogAnalytics
import com.everlog.android.workout.log.WorkoutLogNotification
import com.everlog.android.workout.notification.WorkoutServiceAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the workout screens and service get from the app's graph, through appGraph<WorkoutGraph>()
@ContributesTo(AppScope::class)
interface WorkoutGraph {
    val userSettings: UserSettings

    val ongoingWorkoutRepository: OngoingWorkoutRepository

    val loadPrefillSources: LoadPrefillSourcesUseCase

    // The user's own exercises as they change, so the workout shows a rename straight away
    val exerciseRepository: ExerciseRepository

    val ongoingPlanRepository: OngoingPlanRepository

    // WorkoutService's notification, which the app runs
    val workoutLogNotification: WorkoutLogNotification

    val workoutLogAlerts: WorkoutLogAlerts

    val workoutLogAnalytics: WorkoutLogAnalytics

    val workoutServiceAnalytics: WorkoutServiceAnalytics

    val workoutDetailsAnalytics: WorkoutDetailsAnalytics

    val setSuggestionsSettings: SetSuggestionsSettings

    val setSuggestionsAnalytics: SetSuggestionsAnalytics

    val workoutStats: WorkoutStats

    val workoutDialogs: WorkoutDialogs

    val navigator: Navigator
}
