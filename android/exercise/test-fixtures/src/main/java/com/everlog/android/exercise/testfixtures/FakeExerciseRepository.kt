package com.everlog.android.exercise.testfixtures

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.exercise.data.model.library.ExerciseRegistry
import com.everlog.android.exercise.data.model.library.LibraryExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

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
