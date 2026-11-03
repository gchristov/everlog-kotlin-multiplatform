package com.everlog.application.features

import arrow.core.Either
import com.everlog.android.exercise.data.ExerciseUsage
import com.everlog.android.template.data.RoutineRepository
import com.everlog.android.workout.data.WorkoutRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExerciseUsage(
    private val routines: RoutineRepository,
    private val workouts: WorkoutRepository,
) : ExerciseUsage {

    override suspend fun templateCounts(): Either<Throwable, Map<String, Int>> = routines.exerciseTemplateCounts()

    override suspend fun lastDone(): Either<Throwable, Map<String, Long>> = workouts.exerciseLastDone()
}
