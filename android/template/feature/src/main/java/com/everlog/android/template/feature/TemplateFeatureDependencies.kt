package com.everlog.android.template.feature

import android.app.Activity
import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType

/**
 * What the template feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [TemplateFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface TemplateFeatureDependencies {
    fun templateAnalytics(): TemplateAnalytics

    // Opens the template editor for [routine], or for a new template when null, with TemplateRequestCodes.EDIT
    fun openTemplateEditor(activity: Activity, routine: ELRoutine?)

    fun startWorkout(activity: Activity, routine: ELRoutine, fromPlan: Boolean)

    fun openExerciseDetails(context: Context, exercise: ELExercise)

    // Takes a deleted template out of the plan in progress
    fun removeFromOngoingPlan(activity: Activity, routine: ELRoutine)

    // Opens the set types feedback form, or null when Remote Config has no form for it
    fun setTypesFeedback(activity: Activity): (() -> Unit)?
}

interface TemplateAnalytics {
    fun routineDeleted()
    fun setTypeSelected(type: ELSetType?)
}

object TemplateFeature {
    lateinit var dependencies: TemplateFeatureDependencies
}

// What the template screens are opened with and hand back. The app reads the same names.
object TemplateExtras {
    const val ROUTINE = "EXTRA_ROUTINE"
    const val VIEW_ONLY = "EXTRA_VIEW_ONLY"
    const val FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
    const val SET_TYPE = "EXTRA_SET_TYPE"
    const val EXERCISE_COUNT = "EXTRA_NUMBER_OF_ITEMS"
}

object TemplateRequestCodes {
    const val PICK_ROUTINE = 2
    const val PICK_SET_TYPE = 3
    const val EDIT = 6
}

// The names analytics already has for the template screens
object TemplateScreenNames {
    const val DETAILS = "screen_routine_details"
    const val PERFORM = "screen_perform_routine"
    const val PICKER = "screen_routine_picker"
    const val SET_TYPE_PICKER = "screen_set_type_picker"
}
