package com.everlog.android.template

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab

// The other screens the template screens open, through the app's Navigator, and what they hand back
interface TemplateNavigation {
    // Opens the template editor for [routine], or for a new template when null, with TemplateRequestCodes.EDIT
    fun openTemplateEditor(activity: Activity, routine: ELRoutine?)

    fun startWorkout(activity: Activity, routine: ELRoutine, fromPlan: Boolean)

    fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab = ExerciseTab.Info)

    fun openExercisePicker(activity: Activity)

    // The exercises the exercise picker hands back, from onActivityResult
    fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>?

    // Opens the set types feedback form, or null when Remote Config has no form for it
    fun setTypesFeedback(activity: Activity): (() -> Unit)?
}
