package com.everlog.application.features

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.data.ExerciseCopies
import com.everlog.data.migration.MultiPartUpdateExercise
import com.everlog.managers.firebase.FirestorePathManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExerciseCopies : ExerciseCopies {
    override fun update(exercise: ELExercise) = MultiPartUpdateExercise(exercise).migrate(FirestorePathManager)
}
