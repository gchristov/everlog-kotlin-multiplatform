package com.everlog.android.exercise.data

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.everlog.android.auth.data.UserSession
import com.everlog.android.auth.data.requireUserId
import com.everlog.android.common.data.model.exercise.ELExercise
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Deletes one of the user's own exercises and its image. Workouts and routines keep their copy of it, so history
 * still shows it. Fails without a signed-in user, or when the delete is rejected.
 */
interface DeleteExerciseUseCase {
    suspend operator fun invoke(exercise: ELExercise): Either<Throwable, Unit>
}

@ContributesBinding(AppScope::class)
@Inject
class RealDeleteExerciseUseCase(
    private val exerciseRepository: ExerciseRepository,
    private val imageStorage: ExerciseImageStorage,
    private val session: UserSession,
) : DeleteExerciseUseCase {

    override suspend operator fun invoke(exercise: ELExercise): Either<Throwable, Unit> = either {
        val uuid = ensureNotNull(exercise.uuid) { IllegalArgumentException("An exercise without a uuid") }
        val userId = Either.catch { session.requireUserId() }.bind()
        exerciseRepository.deleteOwnExercise(uuid).bind()
        imageStorage.deleteExerciseImage(exercise, userId)
    }
}
