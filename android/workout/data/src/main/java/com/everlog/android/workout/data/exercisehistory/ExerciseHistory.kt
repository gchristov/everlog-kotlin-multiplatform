package com.everlog.android.workout.data.exercisehistory

import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import kotlinx.coroutines.flow.Flow

/**
 * One time an exercise was done: its sets in a finished workout. Keeps only what the history shows of the workout,
 * not the workout, so older years hold little however far back the user goes.
 */
data class ExerciseSession(
    val workoutUuid: String,
    val workoutName: String?,
    val completedDate: Long,
    val exercise: ELRoutineExercise,
)

/**
 * An exercise's sessions shown so far, most recently done first: the last months as they change, then a year at a
 * time further back as the user scrolls.
 *
 * @param loadingMore An older year is being read.
 * @param complete Nothing older: the history goes back to the user's first workout.
 * @param failed The last older year couldn't be read, e.g. offline before it ever synced: loadMore tries again.
 */
data class ExerciseHistory(
    val sessions: List<ExerciseSession>,
    val loadingMore: Boolean,
    val complete: Boolean,
    val failed: Boolean,
)

// One screen's read of an exercise's history. The last months follow the cache, so a workout saved shows in place.
interface ExerciseHistoryRead {
    val loads: Flow<Load<ExerciseHistory>>

    // For a screen scrolled to its oldest session: the year before, or the first one further back with the exercise
    fun loadMore()
}

// Each time the workout logged the exercise, leaving out the times it was in the workout but never done
internal fun ELWorkout.sessionsOf(exerciseUuid: String): List<ExerciseSession> {
    val workoutUuid = uuid ?: return emptyList()
    if (completedDate <= 0) return emptyList()
    return getExerciseGroups()
        .flatMap { it.exercises }
        .filter { it.exercise?.uuid == exerciseUuid && it.getSetsWithData().isNotEmpty() }
        .map { ExerciseSession(workoutUuid, getName(), completedDate, it) }
}
