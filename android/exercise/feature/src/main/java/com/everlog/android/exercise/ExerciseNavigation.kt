package com.everlog.android.exercise

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType

// The other screens the exercise screens open, through the app's Navigator, and what they hand back
interface ExerciseNavigation {
    fun openExerciseDetails(activity: Activity, exercise: ELExercise)

    fun openCreateExercise(activity: Activity, name: String?)

    fun openSetTypePicker(activity: Activity, exerciseCount: Int)

    // Opens the feedback form, or null when Remote Config has no form for it, so the screen leaves its entry out
    fun feedbackForm(activity: Activity, form: ExerciseFeedbackForm): (() -> Unit)?

    // Explains a missing permission, with a way to the app's system settings
    fun promptForAppSettings(context: Context, rationaleResId: Int)

    // What the set type picker and the create exercise screen hand back, from onActivityResult
    fun pickedSetType(requestCode: Int, resultCode: Int, data: Intent?): ELSetType?

    fun createdExerciseUuid(requestCode: Int, resultCode: Int, data: Intent?): String?
}
