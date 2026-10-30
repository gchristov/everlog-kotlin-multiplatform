package com.everlog.android.exercise.data

import com.everlog.android.common.data.model.exercise.ELExercise

/**
 * What the exercise data needs from the app. The app implements it and registers it in `ELApplication.onCreate`,
 * as with the features' dependencies (see `android/README.md`).
 */
interface ExerciseDataDependencies {
    // Puts the exercise's new name and image into the user's routines and workouts, which keep copies of it
    fun updateCopies(exercise: ELExercise)
}

object ExerciseData {
    lateinit var dependencies: ExerciseDataDependencies
}
