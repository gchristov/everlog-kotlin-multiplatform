package com.everlog.android.workout

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.prefill.LoadPrefillSourcesUseCase
import com.everlog.android.workout.details.WorkoutDetailsAnalytics
import com.everlog.android.workout.details.WorkoutTextField
import com.everlog.android.workout.log.WorkoutLogAlerts
import com.everlog.android.workout.log.WorkoutLogAnalytics
import com.everlog.android.workout.log.WorkoutLogNotification
import com.everlog.android.workout.notification.WorkoutServiceAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import java.util.Date
import kotlinx.coroutines.flow.Flow
import rx.Observable

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

    fun workoutDetailsAnalytics(): WorkoutDetailsAnalytics

    fun shareWorkout(activity: Activity, workout: ELWorkout)

    // Asks for a rating after a finished workout, when the rating prompt's rules allow it
    fun requestRatePrompt(activity: Activity, workout: ELWorkout, delayMillis: Long)

    // Puts [routine] in place of the ongoing plan's copy of it, if it's in the plan
    fun replaceRoutineInOngoingPlan(context: Context, routine: ELRoutine)

    // How many of [workouts] were done this week, as the statistics count them
    fun workoutsThisWeek(workouts: List<ELWorkout>, onCount: (Int) -> Unit, onError: (Throwable) -> Unit)

    fun showTextPrompt(context: Context, value: String?, field: WorkoutTextField): Observable<String>

    fun showDateTimePrompt(context: Context, date: Date): Observable<Date>

    // The exercise groups editor, for a finished workout's exercises
    fun openExerciseEditor(activity: Activity, groups: List<ELExerciseGroup>)

    // What the exercise groups editor hands back, from onActivityResult
    fun editedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>?
}

object WorkoutFeature {
    lateinit var dependencies: WorkoutFeatureDependencies
}

// What the workout screens are opened with. The app reads the same names.
object WorkoutExtras {
    const val WORKOUT = "EXTRA_WORKOUT"
    const val FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
    const val JUST_FINISHED = "EXTRA_WORKOUT_JUST_FINISHED"
}

// The names analytics already has for the workout screens
object WorkoutScreenNames {
    const val LOG = "screen_workout_v2"
    const val SET_SUGGESTIONS = "screen_settings_set_suggestions"
    const val DETAILS = "screen_workout_details"
}
