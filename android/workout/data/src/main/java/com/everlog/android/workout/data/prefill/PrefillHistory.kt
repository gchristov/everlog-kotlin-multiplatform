package com.everlog.android.workout.data.prefill

import arrow.core.Either
import arrow.core.raise.either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.workout.data.prefill.WorkoutPrefillController.Companion.ORM_WINDOW_MILLIS
import java.util.concurrent.TimeUnit

// Workouts are read by when they were created but ordered by when they were completed, so one created before a
// window could still have been completed after a session found in it, if it ran longer than this
internal val LongestWorkoutMillis = TimeUnit.DAYS.toMillis(7)

internal val OlderWindowMillis = TimeUnit.DAYS.toMillis(365)

/**
 * The workouts the [exercises]' prefill needs, read back from [now] a window at a time, as a long history doesn't
 * fit in memory at once: the 1RM window, then older windows for the exercises still missing their last session,
 * until each has one or [historyStart] is reached. Anything older than that is read last, in case the start is
 * wrong. Only workouts with the exercises it's looking for are kept.
 *
 * @param read The workouts created from a time until before another, or until now when that's null
 */
internal suspend fun prefillHistory(
    exercises: List<ELExercise>,
    now: Long,
    historyStart: Long,
    read: suspend (from: Long, until: Long?) -> Either<Throwable, List<ELWorkout>>,
): Either<Throwable, List<ELWorkout>> = either {
    var until = now - ORM_WINDOW_MILLIS - LongestWorkoutMillis
    val kept = read(until, null).bind().withSessionsOf(exercises).toMutableList()
    var missing = exercises.missingLastSession(kept, until)
    while (missing.isNotEmpty() && until > historyStart) {
        val from = maxOf(until - OlderWindowMillis, historyStart)
        kept += read(from, until).bind().withSessionsOf(missing)
        until = from
        missing = exercises.missingLastSession(kept, until)
    }
    if (missing.isNotEmpty()) {
        kept += read(Long.MIN_VALUE, until).bind().withSessionsOf(missing)
    }
    kept
}

// The exercises whose last session could still be in a workout created before [readFrom]
private fun List<ELExercise>.missingLastSession(read: List<ELWorkout>, readFrom: Long): List<ELExercise> = filter { exercise ->
    val lastDone = read.filter { it.sessionsOf(exercise).isNotEmpty() }.maxOfOrNull { it.completedDate }
    lastDone == null || lastDone - readFrom < LongestWorkoutMillis
}

private fun List<ELWorkout>.withSessionsOf(exercises: List<ELExercise>): List<ELWorkout> =
    filter { workout -> exercises.any { workout.sessionsOf(it).isNotEmpty() } }
