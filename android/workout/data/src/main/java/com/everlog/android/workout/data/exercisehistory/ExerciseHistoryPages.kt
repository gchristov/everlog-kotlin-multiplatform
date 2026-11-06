package com.everlog.android.workout.data.exercisehistory

import arrow.core.Either
import arrow.core.raise.either
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * An exercise's history: [recent], the workouts followed from [recentFrom] on, then a year at a time before it
 * through [readBetween], keeping only the exercise's sessions. A year without them reads the one before straight away,
 * until [oldest], the user's first workout, read once.
 *
 * @param recentFrom Where [recent] starts, or later: a workout read both ways shows once
 */
internal class ExerciseHistoryPages(
    private val exerciseUuid: String,
    private val recent: Flow<Load<List<ELWorkout>>>,
    private val recentFrom: Long,
    private val oldest: suspend () -> Either<Throwable, Long?>,
    private val readBetween: suspend (from: Long, until: Long) -> Either<Throwable, List<ELWorkout>>,
    private val dispatcher: CoroutineDispatcher,
) : ExerciseHistoryRead {

    private val older = MutableStateFlow(Older(until = recentFrom))
    private val requests = Channel<Unit>(Channel.CONFLATED)

    override val loads: Flow<Load<ExerciseHistory>> = channelFlow {
        launch { for (request in requests) loadOlder() }
        val recentSessions = recent.map { load -> load.map { workouts -> workouts.flatMap { it.sessionsOf(exerciseUuid) } } }.flowOn(dispatcher)
        combine(recentSessions, older) { recent, older ->
            when (recent) {
                is Load.Ready -> Load.Ready(history(recent.value, older))
                is Load.Unavailable -> Load.Unavailable(recent.error)
                Load.Loading -> Load.Loading
            }
        }.collectLatest { send(it) }
    }

    // Asked again while a year is on its way, it doesn't read another after it
    override fun loadMore() {
        if (!older.value.loadingMore) requests.trySend(Unit)
    }

    private suspend fun loadOlder() {
        val start = older.value
        if (start.complete) return
        older.update { it.copy(loadingMore = true, failed = false) }
        readBack(start).fold(
            { older.update { it.copy(loadingMore = false, failed = true) } },
            { read -> older.update { it.copy(sessions = it.sessions + read.sessions, until = read.until, oldest = read.oldest, complete = read.complete, loadingMore = false) } },
        )
    }

    // From where the last read stopped, a year at a time, until a year has the exercise or there's nothing older
    private suspend fun readBack(start: Older): Either<Throwable, ReadBack> = either {
        val oldest = start.oldest ?: Oldest(oldest().bind())
        val sessions = mutableListOf<ExerciseSession>()
        var until = start.until
        while (sessions.isEmpty() && !reachedFirst(until, oldest)) {
            val from = max(oldest.date!!, until - YearMillis)
            val workouts = readBetween(from, until).bind()
            sessions += withContext(dispatcher) { workouts.flatMap { it.sessionsOf(exerciseUuid) } }
            until = from
        }
        ReadBack(sessions, until, oldest, complete = reachedFirst(until, oldest))
    }

    private fun reachedFirst(until: Long, oldest: Oldest) = oldest.date == null || until <= oldest.date

    private fun history(recent: List<ExerciseSession>, older: Older): ExerciseHistory {
        val recentWorkouts = recent.mapTo(HashSet()) { it.workoutUuid }
        val sessions = (recent + older.sessions.filter { it.workoutUuid !in recentWorkouts }).sortedByDescending { it.completedDate }
        return ExerciseHistory(sessions, older.loadingMore, older.complete, older.failed)
    }

    // The first workout's date once read, null when there are no workouts
    private data class Oldest(val date: Long?)

    private data class Older(
        val until: Long,
        val sessions: List<ExerciseSession> = emptyList(),
        val oldest: Oldest? = null,
        val loadingMore: Boolean = false,
        val complete: Boolean = false,
        val failed: Boolean = false,
    )

    private class ReadBack(val sessions: List<ExerciseSession>, val until: Long, val oldest: Oldest, val complete: Boolean)

    private companion object {
        val YearMillis = TimeUnit.DAYS.toMillis(365)
    }
}
