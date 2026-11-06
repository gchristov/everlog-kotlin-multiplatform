package com.everlog.android.workout.testfixtures

import arrow.core.Either
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.workout.data.WorkoutHistory
import com.everlog.android.workout.data.WorkoutHistoryRead
import com.everlog.android.workout.data.WorkoutRepository
import com.everlog.android.workout.data.exercisehistory.ExerciseHistory
import com.everlog.android.workout.data.exercisehistory.ExerciseHistoryRead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Workouts set by the test: [recent] for any number of months, unless [synced] or a month count in [byMonths] has
// its own, and [history] for the paged read. Each is Loading until the test says otherwise.
class FakeWorkoutRepository : WorkoutRepository {
    val recent = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)
    var synced: MutableStateFlow<Load<List<ELWorkout>>>? = null
    val byMonths = mutableMapOf<Int, MutableStateFlow<Load<List<ELWorkout>>>>()

    // How many reads of each month count have started, for a retry
    val reads = mutableMapOf<Int, Int>()

    val history = FakeWorkoutHistoryRead()

    // How many history reads have started, to tell a read dropped and started again
    var historyReads = 0
        private set

    override fun history(pageSize: Int): WorkoutHistoryRead {
        historyReads++
        return history
    }

    override fun syncedWorkouts(): Flow<Load<List<ELWorkout>>> = synced ?: recent

    override fun recentWorkouts(months: Int): Flow<Load<List<ELWorkout>>> {
        reads[months] = (reads[months] ?: 0) + 1
        return byMonths[months] ?: recent
    }

    override suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>> =
        Either.Right(emptyList())

    override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> = Either.Right(emptyMap())

    val exerciseHistory = FakeExerciseHistoryRead()

    // The exercises whose history was read, by uuid
    val exerciseHistoryReads = mutableListOf<String>()

    override fun exerciseHistory(exerciseUuid: String): ExerciseHistoryRead {
        exerciseHistoryReads += exerciseUuid
        return exerciseHistory
    }
}

// An exercise's history as the test sets it, counting the years asked for
class FakeExerciseHistoryRead : ExerciseHistoryRead {
    override val loads = MutableStateFlow<Load<ExerciseHistory>>(Load.Loading)

    var loadMoreCalls = 0
        private set

    override fun loadMore() {
        loadMoreCalls++
    }
}

// The history as the test sets it, counting the pages asked for
class FakeWorkoutHistoryRead : WorkoutHistoryRead {
    override val loads = MutableStateFlow<Load<WorkoutHistory>>(Load.Loading)

    var loadMoreCalls = 0
        private set

    override fun loadMore() {
        loadMoreCalls++
    }
}
