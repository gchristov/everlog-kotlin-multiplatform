package com.everlog.data.controllers.exerciselibrary

import arrow.core.getOrElse
import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.LoadExerciseLibraryUseCase
import com.everlog.android.exercise.data.exerciseLibraryOf
import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import com.everlog.data.repositories.ExerciseRepository
import com.everlog.data.repositories.RoutineRepository
import com.everlog.data.repositories.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class RealLoadExerciseLibraryUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : LoadExerciseLibraryUseCase {

    override operator fun invoke(): Flow<Load<ExerciseLibrary>> = flow {
        val (counts, lastDone) = coroutineScope {
            val countsRead = async { routineRepository.exerciseTemplateCounts() }
            val lastDoneRead = async { workoutRepository.exerciseLastDone() }
            countsRead.await().getOrElse { warn("template counts", it); emptyMap() } to
                lastDoneRead.await().getOrElse { warn("recent workouts", it); emptyMap() }
        }
        emitAll(
            combine(exerciseRepository.libraryExercises(), exerciseRepository.ownExercises(), exerciseRepository.registry()) { global, own, registry ->
                exerciseLibraryOf(global, own, registry, counts, lastDone)
            }
        )
    }.flowOn(dispatcher)

    private fun warn(what: String, error: Throwable) {
        Timber.tag(TAG).w(error, "Loaded the exercise library without its %s", what)
    }

    private companion object {
        const val TAG = "LoadExerciseLibrary"
    }
}
