package com.everlog.android.workout

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.prefill.LoadPrefillSourcesUseCase
import com.everlog.android.workout.log.WorkoutLogAlerts
import com.everlog.android.workout.log.WorkoutLogAnalytics
import com.everlog.android.workout.log.WorkoutLogNotification
import com.everlog.android.workout.notification.WorkoutServiceAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import kotlinx.coroutines.flow.Flow

/**
 * What the workout feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [WorkoutFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface WorkoutFeatureDependencies {
    fun ongoingWorkoutRepository(): OngoingWorkoutRepository

    fun loadPrefillSources(): LoadPrefillSourcesUseCase

    // The user's own exercises as they change, so the workout shows a rename straight away
    fun ownExerciseUpdates(): Flow<List<LibraryExercise>>

    fun workoutLogAnalytics(): WorkoutLogAnalytics

    // WorkoutService's notification, which the app runs
    fun workoutNotification(context: Context): WorkoutLogNotification

    fun timerAlerts(context: Context): WorkoutLogAlerts

    fun workoutServiceAnalytics(): WorkoutServiceAnalytics

    fun openExercisePicker(activity: Activity)

    // The exercises the exercise picker hands back, from onActivityResult
    fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>?

    fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab)

    // Set suggestions, or the old muscle goal screen while Remote Config's set_suggestions_v2 is off
    fun openSetSuggestions(activity: Activity)

    fun openWorkoutDetails(activity: Activity, workout: ELWorkout, fromPlan: Boolean)

    // Explains a missing permission, with a way to the app's system settings
    fun promptForAppSettings(context: Context, rationaleResId: Int)

    // Opens a feedback form, or null when Remote Config has no form for it, so the screen leaves its entry out
    fun workoutFeedback(activity: Activity): (() -> Unit)?

    fun setSuggestionsFeedback(activity: Activity): (() -> Unit)?

    fun setSuggestionsSettings(context: Context): SetSuggestionsSettings

    fun setSuggestionsAnalytics(): SetSuggestionsAnalytics
}

object WorkoutFeature {
    lateinit var dependencies: WorkoutFeatureDependencies
}

// What the workout screens are opened with. The app reads the same names.
object WorkoutExtras {
    const val WORKOUT = "EXTRA_WORKOUT"
    const val FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
}

// The names analytics already has for the workout screens
object WorkoutScreenNames {
    const val LOG = "screen_workout_v2"
    const val SET_SUGGESTIONS = "screen_settings_set_suggestions"
}
