package com.everlog.android.exercise.feature

import android.app.Activity
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType
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

    // Opens the feedback form, or null when Remote Config has no form for it, so the picker leaves its entry out
    fun sendFeedback(activity: Activity, fromNoResults: Boolean): (() -> Unit)?

    // What the set type picker and the create exercise screen hand back, from onActivityResult
    fun pickedSetType(requestCode: Int, resultCode: Int, data: Intent?): ELSetType?

    fun createdExerciseUuid(requestCode: Int, resultCode: Int, data: Intent?): String?
}

object ExerciseFeature {
    lateinit var dependencies: ExerciseFeatureDependencies
}
