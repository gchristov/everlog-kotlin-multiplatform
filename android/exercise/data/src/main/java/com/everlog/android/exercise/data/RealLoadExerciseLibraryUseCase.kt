package com.everlog.android.exercise.data

import arrow.core.getOrElse
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

@ContributesBinding(AppScope::class)
@Inject
class RealLoadExerciseLibraryUseCase(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
    private val usage: ExerciseUsage,
) : LoadExerciseLibraryUseCase {

    override operator fun invoke(): Flow<Load<ExerciseLibrary>> = flow {
        val (counts, lastDone) = coroutineScope {
            val countsRead = async { usage.templateCounts() }
            val lastDoneRead = async { usage.lastDone() }
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
