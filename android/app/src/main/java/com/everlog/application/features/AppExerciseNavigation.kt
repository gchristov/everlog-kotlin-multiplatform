package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.exercise.ExerciseFeedbackForm
import com.everlog.android.exercise.ExerciseNavigation
import com.everlog.android.exercise.details.ExerciseDetailsActivity
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExerciseNavigation : ExerciseNavigation {
    override fun openExerciseDetails(activity: Activity, exercise: ELExercise) =
        ELNavigator(activity).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise))

    override fun openCreateExercise(activity: Activity, name: String?) = ELNavigator(activity).openCreateExercise(name)

    override fun openSetTypePicker(activity: Activity, exerciseCount: Int) = ELNavigator(activity).openSetTypePicker(exerciseCount)

    override fun feedbackForm(activity: Activity, form: ExerciseFeedbackForm): (() -> Unit)? = activity.feedbackFormLauncher(
        when (form) {
            ExerciseFeedbackForm.PickerMenu -> FeedbackSource.EXERCISE_PICKER_MENU
            ExerciseFeedbackForm.PickerNoResults -> FeedbackSource.EXERCISE_PICKER_NO_RESULTS
            ExerciseFeedbackForm.Statistics -> FeedbackSource.EXERCISE_STATISTICS
        },
    )

    override fun promptForAppSettings(context: Context, rationaleResId: Int) = ELNavigator(context).promptForAppSettings(rationaleResId)

    override fun pickedSetType(requestCode: Int, resultCode: Int, data: Intent?): ELSetType? =
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_SET_TYPE && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            data?.getSerializableExtra(ELConstants.EXTRA_SET_TYPE) as? ELSetType
        } else {
            null
        }

    override fun createdExerciseUuid(requestCode: Int, resultCode: Int, data: Intent?): String? =
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_EXERCISE && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(ELConstants.EXTRA_EXERCISE_UUID)
        } else {
            null
        }
}
