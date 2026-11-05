package com.everlog.android.workout.testfixtures

import arrow.core.Either
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.workout.data.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Recent workouts set by the test through [recent]: Loading until it says otherwise
class FakeWorkoutRepository : WorkoutRepository {
    val recent = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)

    override fun recentWorkouts(months: Int): Flow<Load<List<ELWorkout>>> = recent

    override suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>> =
        Either.Right(emptyList())

    override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> = Either.Right(emptyMap())
}
