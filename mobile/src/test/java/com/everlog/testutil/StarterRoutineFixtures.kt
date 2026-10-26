package com.everlog.testutil

import arrow.core.Either
import com.everlog.data.controllers.starterroutines.StarterWeek
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.repositories.ExerciseRepository
import com.everlog.data.repositories.RoutineRepository
import com.everlog.data.sync.Load
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * The week's exercises as they'd come from the exercise library.
 */
fun exerciseLibrary(week: StarterWeek): List<ELExercise> = week.routines.flatMap { it.exercises }
    .distinctBy { it.exerciseId }
    .map { ELExercise(uuid = it.exerciseId) }

class FakeExerciseRepository(
    var globalExercises: Either<Throwable, List<ELExercise>> = Either.Right(emptyList()),
    var libraryExercise: Load<LibraryExercise?> = Load.Ready(null),
) : ExerciseRepository {
    var globalExercisesCalls = 0
        private set

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> {
        globalExercisesCalls++
        return globalExercises
    }

    override fun libraryExercises(): Flow<Load<List<LibraryExercise>>> = flowOf(Load.Ready(emptyList()))

    // The uuids asked for, in order
    val libraryExerciseReads = mutableListOf<String>()

    override fun libraryExercise(uuid: String): Flow<Load<LibraryExercise?>> {
        libraryExerciseReads += uuid
        return flowOf(libraryExercise)
    }

    override fun ownExercises(): Flow<Load<List<LibraryExercise>>> = flowOf(Load.Ready(emptyList()))

    override fun registry(): Flow<Load<ExerciseRegistry?>> = flowOf(Load.Ready(ExerciseRegistry()))

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> = emptyFlow()
}

class FakeRoutineRepository(
    var saveRoutines: Either<Throwable, Unit> = Either.Right(Unit),
) : RoutineRepository {
    // The routines of each save, in order
    val saved = mutableListOf<List<ELRoutine>>()

    override suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit> {
        saved += routines
        return saveRoutines
    }

    override suspend fun exerciseTemplateCounts(): Either<Throwable, Map<String, Int>> = Either.Right(emptyMap())
}
