package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.LoadExerciseLibraryUseCase
import com.everlog.android.exercise.data.RealExerciseRepository
import com.everlog.android.exercise.data.RealLoadExerciseGuideUseCase
import com.everlog.android.exercise.data.RealLoadExerciseLibraryUseCase
import com.everlog.android.exercise.feature.ExerciseAnalytics
import com.everlog.android.exercise.feature.ExerciseFeatureDependencies
import com.everlog.android.exercise.feature.ExerciseFeedbackForm
import com.everlog.android.exercise.feature.picker.ExercisePickerAnalytics
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.android.workout.data.RealWorkoutRepository
import com.everlog.config.AppConfig
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.RemoteConfigManager
import com.everlog.android.exercise.feature.details.ExerciseDetailsActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import kotlinx.coroutines.Dispatchers

class AppExerciseFeatureDependencies : ExerciseFeatureDependencies {

    override fun loadExerciseLibrary(): LoadExerciseLibraryUseCase {
        val io = Dispatchers.IO
        return RealLoadExerciseLibraryUseCase(
            io,
            RealExerciseRepository(io, AppDataSync.instance),
            RealRoutineRepository(io, AppDataSync.instance) { ELDatastore.routineStore() },
            RealWorkoutRepository(io, AppDataSync.instance, AppConfig.configuration.workoutSyncMonths),
        )
    }

    override fun pickerAnalytics(): ExercisePickerAnalytics = AppExercisePickerAnalytics()

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

    override fun exerciseAnalytics(): ExerciseAnalytics = AppExerciseAnalytics()

    override fun loadExerciseGuide(): LoadExerciseGuideUseCase {
        val io = Dispatchers.IO
        return RealLoadExerciseGuideUseCase(io, RealExerciseRepository(io, AppDataSync.instance))
    }

    override fun workoutsStore(): CollectionStore<ELWorkout> = ELDatastore.workoutsStore()

    override fun exerciseDetailsV2(): Boolean = RemoteConfigManager.manager.exerciseDetailsV2()

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
