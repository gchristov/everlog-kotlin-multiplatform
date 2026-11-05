package com.everlog.android.workout.testfixtures

import arrow.core.Either
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.workout.data.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Workouts set by the test: [recent] for any number of months, unless [synced] or a month count in [byMonths] has
// its own. Each is Loading until the test says otherwise.
class FakeWorkoutRepository : WorkoutRepository {
    val recent = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)
    var synced: MutableStateFlow<Load<List<ELWorkout>>>? = null
    val byMonths = mutableMapOf<Int, MutableStateFlow<Load<List<ELWorkout>>>>()

    // How many reads of each month count have started, for a retry
    val reads = mutableMapOf<Int, Int>()

    override fun syncedWorkouts(): Flow<Load<List<ELWorkout>>> = synced ?: recent

    override fun recentWorkouts(months: Int): Flow<Load<List<ELWorkout>>> {
        reads[months] = (reads[months] ?: 0) + 1
        return byMonths[months] ?: recent
    }

    override suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>> =
        Either.Right(emptyList())

    override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> = Either.Right(emptyMap())
}
