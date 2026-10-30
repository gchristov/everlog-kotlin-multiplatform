package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datasync.CollectionSource
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.datasync.FirestoreCollection
import com.everlog.android.common.datasync.Range
import com.everlog.android.common.datasync.SyncPlan
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.kotlin.Load
import com.everlog.config.AppConfig
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

interface WorkoutRepository {
    /**
     * The user's workouts, newest first, as they change. [Range.All] waits for the full history the first time
     * after an install, then shows the cache while it syncs again weekly; a range within the last
     * [AppConfig.workoutSyncMonths] never waits for it. None without a signed-in user.
     */
    fun workouts(range: Range): Flow<Load<List<ELWorkout>>>

    /**
     * When the user last did each exercise, by the exercise's uuid, from their [workouts] most recent workouts
     * of the last months.
     */
    suspend fun exerciseLastDone(workouts: Int = RecentWorkouts): Either<Throwable, Map<String, Long>>

    companion object {
        const val RecentWorkouts = 30
    }
}

class RealWorkoutRepository(
    private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
) : WorkoutRepository {

    override fun workouts(range: Range): Flow<Load<List<ELWorkout>>> {
        val userId = LocalUserManager.getUser()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(
            CollectionSource(
                "workouts",
                userId,
                SyncPlan.Recent(dateField = FirestoreFields.CREATED_DATE, months = AppConfig.configuration.workoutSyncMonths),
                FirestoreCollection(
                    FirestorePathManager.workoutsCollection,
                    order = { it.orderBy(FirestoreFields.CREATED_DATE, Query.Direction.DESCENDING) },
                    dateField = FirestoreFields.CREATED_DATE,
                    dispatcher = dispatcher,
                ) { it.toObject(ELWorkout::class.java) },
            ),
            range,
        )
    }

    // A first open with nothing synced waits for the recent sync
    override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> =
        when (val load = workouts(Range.Last(AppConfig.configuration.workoutSyncMonths)).first { it != Load.Loading }) {
            is Load.Ready -> Either.Right(exerciseLastDone(load.value.take(workouts)))
            is Load.Unavailable -> Either.Left(load.error)
            Load.Loading -> error("Filtered out")
        }
}

// When each exercise was last done in these workouts, by the exercise's uuid
internal fun exerciseLastDone(workouts: List<ELWorkout>): Map<String, Long> = buildMap {
    workouts.forEach { workout ->
        workout.routine?.exerciseGroups.orEmpty()
            .flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }
            .forEach { uuid -> if (workout.createdDate > (get(uuid) ?: Long.MIN_VALUE)) put(uuid, workout.createdDate) }
    }
}
