package com.everlog.android.exercise.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.LoadExerciseLibraryUseCase
import com.everlog.android.exercise.feature.picker.ExercisePickerAnalytics

/**
 * What the exercise feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [ExerciseFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface ExerciseFeatureDependencies {
    fun loadExerciseLibrary(): LoadExerciseLibraryUseCase

    fun pickerAnalytics(): ExercisePickerAnalytics

    fun openExerciseDetails(activity: Activity, exercise: ELExercise)

    fun openCreateExercise(activity: Activity, name: String?)

    fun openSetTypePicker(activity: Activity, exerciseCount: Int)

    // Opens the feedback form, or null when Remote Config has no form for it, so the screen leaves its entry out
    fun feedbackForm(activity: Activity, form: ExerciseFeedbackForm): (() -> Unit)?

    fun exerciseAnalytics(): ExerciseAnalytics

    fun loadExerciseGuide(): LoadExerciseGuideUseCase

    // The user's workouts, for the exercise's statistics and history
    fun workoutsStore(): CollectionStore<ELWorkout>

    // The exercise detail in Compose (for now its Info tab), from Remote Config
    fun exerciseDetailsV2(): Boolean

    // Explains a missing permission, with a way to the app's system settings
    fun promptForAppSettings(context: Context, rationaleResId: Int)

    // What the set type picker and the create exercise screen hand back, from onActivityResult
    fun pickedSetType(requestCode: Int, resultCode: Int, data: Intent?): ELSetType?

    fun createdExerciseUuid(requestCode: Int, resultCode: Int, data: Intent?): String?
}

object ExerciseFeature {
    lateinit var dependencies: ExerciseFeatureDependencies
}
