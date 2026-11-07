package com.everlog.android.workout.data

import arrow.core.Either
import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.common.kotlin.map
import com.everlog.android.datasync.CollectionSource
import com.everlog.android.datasync.DataSync
import com.everlog.android.datasync.FirestoreCollection
import com.everlog.android.datasync.Range
import com.everlog.android.datasync.SyncPlan
import com.everlog.android.workout.data.exercisehistory.ExerciseHistory
import com.everlog.android.workout.data.exercisehistory.ExerciseHistoryPages
import com.everlog.android.workout.data.exercisehistory.ExerciseHistoryRead
import com.google.firebase.firestore.Query
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.Calendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface WorkoutRepository {
    /**
     * The workouts DataSync keeps synced, the last few months, following the cache as it changes, in no particular
     * order: there whenever they've synced once, without waiting for an older part. Empty without a signed-in user.
     */
    fun syncedWorkouts(): Flow<Load<List<ELWorkout>>>

    /**
     * The user's workouts of the last [months] months, following the cache as it changes, in no particular order:
     * for a screen showing the recent weeks. Empty without a signed-in user.
     */
    fun recentWorkouts(months: Int): Flow<Load<List<ELWorkout>>>

    /**
     * The user's whole history, newest first, a page at a time: for a list the user scrolls back through. Holds only
     * the pages shown so far, so a screen drops the read when the user leaves it. Empty without a signed-in user.
     */
    fun history(pageSize: Int = HistoryPageSize): WorkoutHistoryRead

    /**
     * One exercise's history, by the exercise's uuid: the last [ExerciseHistoryMonths] months as they change, then
     * a year at a time further back, as far as the first workout. Empty without a signed-in user.
     */
    fun exerciseHistory(exerciseUuid: String): ExerciseHistoryRead

    /**
     * The user's workouts created from [from] until before [until], read once and in no particular order: for going
     * through the history a part at a time. Waits the first time it reads further back than the sync months, for
     * that part to sync. None without a signed-in user.
     */
    suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>>

    /**
     * When the user last did each exercise, by the exercise's uuid, from their [workouts] most recent workouts
     * of the last months.
     */
    suspend fun exerciseLastDone(workouts: Int = RecentWorkouts): Either<Throwable, Map<String, Long>>

    companion object {
        const val RecentWorkouts = 30
        const val HistoryPageSize = 50
        const val ExerciseHistoryMonths = 12
    }
}

/**
 * @param syncMonths How many months of workouts to keep synced. Older ones are read when a screen asks for them.
 */
@ContributesBinding(AppScope::class)
@Inject
class RealWorkoutRepository(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
    @WorkoutSyncMonths private val syncMonths: Int,
    private val session: UserSession,
) : WorkoutRepository {

    private fun workouts(range: Range, complete: Boolean): Flow<Load<List<ELWorkout>>> {
        val userId = session.user()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(workoutsOf(userId), range, complete)
    }

    override fun syncedWorkouts(): Flow<Load<List<ELWorkout>>> = workouts(Range.Last(syncMonths), complete = false)

    override fun recentWorkouts(months: Int): Flow<Load<List<ELWorkout>>> = workouts(Range.Last(months), complete = false)

    override fun history(pageSize: Int): WorkoutHistoryRead {
        val userId = session.user()?.id ?: return NoHistory
        val pages = dataSync.pages(workoutsOf(userId), pageSize)
        return object : WorkoutHistoryRead {
            override val loads = pages.loads.map { load -> load.map { WorkoutHistory(it.items, it.loadingMore, it.endReached, it.failed) } }

            override fun loadMore() = pages.loadMore()
        }
    }

    override fun exerciseHistory(exerciseUuid: String): ExerciseHistoryRead {
        val userId = session.user()?.id ?: return NoExerciseHistory
        val source = workoutsOf(userId)
        // The same read as the Activity tab's last 12 months, so the two share one listener when both are open
        val recent = recentWorkouts(WorkoutRepository.ExerciseHistoryMonths)
        return ExerciseHistoryPages(
            exerciseUuid = exerciseUuid,
            recent = recent,
            // After the read above starts, so the years before it overlap it rather than leave a gap
            recentFrom = monthsAgo(WorkoutRepository.ExerciseHistoryMonths),
            oldest = { dataSync.oldest(source).toEither() },
            readBetween = { from, until -> dataSync.read(source, from, until).toEither() },
            dispatcher = dispatcher,
        )
    }

    override suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>> {
        val userId = session.user()?.id ?: return Either.Right(emptyList())
        return dataSync.read(workoutsOf(userId), from, until).toEither()
    }

    private fun workoutsOf(userId: String) = CollectionSource(
        "workouts",
        userId,
        SyncPlan.Recent(dateField = FirestoreFields.CREATED_DATE, months = syncMonths),
        FirestoreCollection(
            FirestorePaths.workouts(userId),
            order = { it.orderBy(FirestoreFields.CREATED_DATE, Query.Direction.DESCENDING) },
            dateField = FirestoreFields.CREATED_DATE,
            dispatcher = dispatcher,
        ) { it.toObject(ELWorkout::class.java) },
    )

    // Waits for the recent sync until there's been one: the cache may hold only single workouts opened before
    override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> =
        when (val load = workouts(Range.Last(syncMonths), complete = true).first { it != Load.Loading }) {
            is Load.Ready -> Either.Right(exerciseLastDone(load.value.take(workouts)))
            is Load.Unavailable -> Either.Left(load.error)
            Load.Loading -> error("Filtered out")
        }
}

private object NoExerciseHistory : ExerciseHistoryRead {
    override val loads = flowOf(Load.Ready(ExerciseHistory(emptyList(), loadingMore = false, complete = true, failed = false)))

    override fun loadMore() = Unit
}

private fun monthsAgo(months: Int): Long = Calendar.getInstance().run {
    add(Calendar.MONTH, -months)
    timeInMillis
}

// A one-off read has waited for its sync, so it's never Loading
private fun <T> Load<T>.toEither(): Either<Throwable, T> = when (this) {
    is Load.Ready -> Either.Right(value)
    is Load.Unavailable -> Either.Left(error)
    Load.Loading -> error("A read waits for its sync")
}

private object NoHistory : WorkoutHistoryRead {
    override val loads = flowOf(Load.Ready(WorkoutHistory(emptyList(), loadingMore = false, complete = true)))

    override fun loadMore() = Unit
}

// When each exercise was last done in these workouts, by the exercise's uuid
internal fun exerciseLastDone(workouts: List<ELWorkout>): Map<String, Long> = buildMap {
    workouts.forEach { workout ->
        workout.routine?.exerciseGroups.orEmpty()
            .flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }
            .forEach { uuid -> if (workout.createdDate > (get(uuid) ?: Long.MIN_VALUE)) put(uuid, workout.createdDate) }
    }
}
