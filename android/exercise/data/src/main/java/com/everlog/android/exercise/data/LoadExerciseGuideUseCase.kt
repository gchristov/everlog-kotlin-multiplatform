package com.everlog.android.exercise.data

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.map
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.exercise.data.ExerciseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * An exercise's [ExerciseGuide] from the library as it changes: empty when the library doesn't have
 * the exercise (a user's own), with blank steps and tips left out.
 *
 * Unavailable when the exercise was never synced and the server can't be reached.
 */
interface LoadExerciseGuideUseCase {
    operator fun invoke(uuid: String): Flow<Load<ExerciseGuide>>
}

class RealLoadExerciseGuideUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
) : LoadExerciseGuideUseCase {

    override operator fun invoke(uuid: String): Flow<Load<ExerciseGuide>> =
        exerciseRepository.libraryExercise(uuid)
            .map { load ->
                load.map { exercise ->
                    ExerciseGuide(
                        instructions = exercise?.instructions.orEmpty().cleaned(),
                        tips = exercise?.tips.orEmpty().cleaned(),
                    )
                }
            }
            .flowOn(dispatcher)

    private fun List<String>.cleaned() = map { it.trim() }.filter { it.isNotEmpty() }
}
