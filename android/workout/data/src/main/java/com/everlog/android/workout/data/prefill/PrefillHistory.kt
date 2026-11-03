package com.everlog.android.workout.data.prefill

import arrow.core.Either
import arrow.core.raise.either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.workout.data.prefill.WorkoutPrefillController.Companion.LAST_SESSION_WINDOW_MILLIS
import com.everlog.android.workout.data.prefill.WorkoutPrefillController.Companion.ORM_WINDOW_MILLIS
import java.util.concurrent.TimeUnit

// Workouts are read by when they were created but ordered by when they were completed, so one created before a
// window could still have been completed after a session found in it, if it ran longer than this
internal val LongestWorkoutMillis = TimeUnit.DAYS.toMillis(7)

/**
 * The workouts the [exercises]' prefill needs, read once rather than followed, as a long history doesn't fit in
 * memory: the 1RM window, then the rest of the year only for the exercises it didn't find there. Only workouts with
 * these exercises are kept.
 *
 * @param read The workouts created from a time until before another, or until now when that's null
 */
internal suspend fun prefillHistory(
    exercises: List<ELExercise>,
    now: Long,
    read: suspend (from: Long, until: Long?) -> Either<Throwable, List<ELWorkout>>,
): Either<Throwable, List<ELWorkout>> = either {
    val recentFrom = now - ORM_WINDOW_MILLIS - LongestWorkoutMillis
    val recent = read(recentFrom, null).bind().withSessionsOf(exercises)
    val missing = exercises.filter { exercise -> !recent.hasLastSession(exercise, recentFrom) }
    if (missing.isEmpty()) return@either recent
    recent + read(now - LAST_SESSION_WINDOW_MILLIS - LongestWorkoutMillis, recentFrom).bind().withSessionsOf(missing)
}

// Whether a workout created before [readFrom] can't hold a later session of the exercise than these
private fun List<ELWorkout>.hasLastSession(exercise: ELExercise, readFrom: Long): Boolean {
    val lastDone = filter { it.sessionsOf(exercise).isNotEmpty() }.maxOfOrNull { it.completedDate } ?: return false
    return lastDone - readFrom >= LongestWorkoutMillis
}

private fun List<ELWorkout>.withSessionsOf(exercises: List<ELExercise>): List<ELWorkout> =
    filter { workout -> exercises.any { workout.sessionsOf(it).isNotEmpty() } }
