package com.everlog.application.features

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.data.ExerciseDataDependencies
import com.everlog.data.migration.MultiPartUpdateExercise
import com.everlog.managers.firebase.FirestorePathManager

class AppExerciseDataDependencies : ExerciseDataDependencies {

    override fun updateCopies(exercise: ELExercise) = MultiPartUpdateExercise(exercise).migrate(FirestorePathManager)
}
