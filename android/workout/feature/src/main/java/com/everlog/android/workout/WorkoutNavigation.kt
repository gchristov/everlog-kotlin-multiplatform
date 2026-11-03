package com.everlog.android.workout

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.navigation.ExerciseTab

// The other screens the workout screens open, through the app's Navigator, and what they hand back
interface WorkoutNavigation {
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

    fun shareWorkout(activity: Activity, workout: ELWorkout)

    // Asks for a rating after a finished workout, when the rating prompt's rules allow it
    fun requestRatePrompt(activity: Activity, workout: ELWorkout, delayMillis: Long)

    // The exercise groups editor, for a finished workout's exercises
    fun openExerciseEditor(activity: Activity, groups: List<ELExerciseGroup>)

    // What the exercise groups editor hands back, from onActivityResult
    fun editedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>?
}
