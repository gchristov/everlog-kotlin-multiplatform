package com.everlog.android.exercise.data

import arrow.core.Either

/**
 * How the user uses each exercise, by its uuid, from their templates and workouts. The app answers it with the
 * template and workout repositories, which build on the exercises rather than the other way round.
 */
interface ExerciseUsage {
    // How many templates use each exercise. An exercise in a template twice counts once.
    suspend fun templateCounts(): Either<Throwable, Map<String, Int>>

    // When each exercise was last done, from the most recent workouts
    suspend fun lastDone(): Either<Throwable, Map<String, Long>>
}
