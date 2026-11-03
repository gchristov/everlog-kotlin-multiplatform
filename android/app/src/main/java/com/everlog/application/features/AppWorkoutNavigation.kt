package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.template.TemplateNavigation
import com.everlog.android.workout.WorkoutNavigation
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.managers.apprate.RatePromptTrigger
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppWorkoutNavigation(
    // The exercise screens open the same way from a template and a workout
    private val templateNavigation: TemplateNavigation,
) : WorkoutNavigation {

    override fun openExercisePicker(activity: Activity) = templateNavigation.openExercisePicker(activity)

    override fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        templateNavigation.pickedExerciseGroups(requestCode, resultCode, data)

    override fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab) =
        templateNavigation.openExerciseDetails(context, exercise, tab)

    override fun openSetSuggestions(activity: Activity) = ELNavigator(activity).openMuscleGoal()

    override fun openWorkoutDetails(activity: Activity, workout: ELWorkout, fromPlan: Boolean) =
        ELNavigator(activity).openWorkoutDetails(workout, true, fromPlan)

    override fun promptForAppSettings(context: Context, rationaleResId: Int) =
        ELNavigator(context).promptForAppSettings(rationaleResId)

    override fun workoutFeedback(activity: Activity): (() -> Unit)? = activity.feedbackFormLauncher(FeedbackSource.WORKOUT_MENU)

    override fun setSuggestionsFeedback(activity: Activity): (() -> Unit)? =
        activity.feedbackFormLauncher(FeedbackSource.SET_SUGGESTIONS)

    override fun shareWorkout(activity: Activity, workout: ELWorkout) = ELNavigator(activity).shareWorkout(workout)

    override fun requestRatePrompt(activity: Activity, workout: ELWorkout, delayMillis: Long) =
        ELNavigator(activity).requestRatePrompt(RatePromptTrigger.WorkoutCompleted(workout), delayMillis)

    override fun openExerciseEditor(activity: Activity, groups: List<ELExerciseGroup>) =
        ELNavigator(activity).openEditExerciseGroups(DefaultCreateExerciseGroupsActivity.Companion.Properties().groups(groups))

    override fun editedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_EXERCISE_GROUPS && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>
        } else {
            null
        }
}
