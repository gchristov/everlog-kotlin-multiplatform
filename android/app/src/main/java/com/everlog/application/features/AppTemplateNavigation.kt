package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.navigation.ExerciseTab
import com.everlog.android.exercise.details.ExerciseDetailsActivity
import com.everlog.android.template.TemplateNavigation
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppTemplateNavigation : TemplateNavigation {
    override fun openTemplateEditor(activity: Activity, routine: ELRoutine?) = ELNavigator(activity).openEditRoutine(
        CreateRoutineActivity.Companion.Properties().routine(routine).showDetailsOnSuccess(false),
    )

    override fun startWorkout(activity: Activity, routine: ELRoutine, fromPlan: Boolean) =
        ELNavigator(activity).startWorkout(routine, true, fromPlan)

    override fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab) {
        val type = when (tab) {
            ExerciseTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
            ExerciseTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
        }
        ELNavigator(context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise).type(type))
    }

    override fun openExercisePicker(activity: Activity) = ELNavigator(activity).openExercisePicker()

    override fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>
        } else {
            null
        }

    override fun setTypesFeedback(activity: Activity): (() -> Unit)? = activity.feedbackFormLauncher(FeedbackSource.SET_TYPES)
}
