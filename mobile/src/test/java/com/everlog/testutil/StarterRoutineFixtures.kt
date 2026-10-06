package com.everlog.testutil

import arrow.core.Either
import com.everlog.data.controllers.starterroutines.StarterWeek
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.repositories.ExerciseRepository

/**
 * The week's exercises as they'd come from the exercise library.
 */
fun exerciseLibrary(week: StarterWeek): List<ELExercise> = week.routines.flatMap { it.exercises }
    .distinctBy { it.exerciseId }
    .map { ELExercise(uuid = it.exerciseId, name = it.name) }

class FakeExerciseRepository(
    var globalExercises: Either<Throwable, List<ELExercise>> = Either.Right(emptyList()),
) : ExerciseRepository {
    var globalExercisesCalls = 0
        private set

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> {
        globalExercisesCalls++
        return globalExercises
    }
}
