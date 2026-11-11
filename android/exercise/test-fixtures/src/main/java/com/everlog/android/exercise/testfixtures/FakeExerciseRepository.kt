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
import java.util.concurrent.CopyOnWriteArrayList

class FakeExerciseRepository(
    var globalExercises: Either<Throwable, List<ELExercise>> = Either.Right(emptyList()),
    var libraryExercise: Load<LibraryExercise?> = Load.Ready(null),
    var library: Load<List<LibraryExercise>> = Load.Ready(emptyList()),
    var own: Load<List<LibraryExercise>> = Load.Ready(emptyList()),
) : ExerciseRepository {
    var globalExercisesCalls = 0
        private set

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> {
        globalExercisesCalls++
        return globalExercises
    }

    override fun libraryExercises(): Flow<Load<List<LibraryExercise>>> = flowOf(library)

    // The uuids asked for, in order
    val libraryExerciseReads = mutableListOf<String>()

    override fun libraryExercise(uuid: String): Flow<Load<LibraryExercise?>> {
        libraryExerciseReads += uuid
        return flowOf(libraryExercise)
    }

    override fun ownExercises(): Flow<Load<List<LibraryExercise>>> = flowOf(own)

    override fun registry(): Flow<Load<ExerciseRegistry?>> = flowOf(Load.Ready(ExerciseRegistry()))

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> = emptyFlow()

    // Each call to allExercises gets the next of these, the last one again once they run out
    var allExercises: MutableList<Flow<Load<List<LibraryExercise>>>> = mutableListOf(flowOf(Load.Ready(emptyList())))

    override fun allExercises(): Flow<Load<List<LibraryExercise>>> =
        if (allExercises.size > 1) allExercises.removeAt(0) else allExercises.first()

    // The uuids asked for by each fetch, in order
    val fetches: MutableList<Set<String>> = CopyOnWriteArrayList()
    var fetch: (Set<String>) -> Load<Set<String>> = { Load.Ready(emptySet()) }

    override suspend fun fetchExercises(uuids: Set<String>): Load<Set<String>> {
        fetches += uuids
        return fetch(uuids)
    }

    var deleteResult: Either<Throwable, Unit> = Either.Right(Unit)

    // The uuids of the own exercises deleted, in order
    val deletedExercises = mutableListOf<String>()

    override suspend fun deleteOwnExercise(uuid: String): Either<Throwable, Unit> {
        deletedExercises += uuid
        return deleteResult
    }
}
