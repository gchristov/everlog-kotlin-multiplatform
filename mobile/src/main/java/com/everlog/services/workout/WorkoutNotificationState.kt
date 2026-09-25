package com.everlog.services.workout

import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import kotlin.math.max

/**
 * What the ongoing workout notification should show, independent of how it's rendered or which
 * Android version it's rendered on. See [WorkoutNotificationBuilder] for the rendering.
 */
sealed class WorkoutNotificationState {

    /**
     * The next set to complete, with controls to tweak it.
     */
    data class NextSet(
            val exerciseName: String,
            // 1-based position of the exercise within a super/giant set, and how many exercises it has.
            val exercisePosition: Int,
            val exercisesInGroup: Int,
            // Raw ELSetType name, e.g. "SUPER"
            val setType: String,
            // 1-based
            val setNumber: Int,
            val totalSets: Int,
            val weight: Float,
            // Null when the set isn't rep based, so the reps controls are hidden.
            val reps: Int?,
            // Null when the set isn't time based, so the time controls are hidden.
            val timeSeconds: Int?,
            val timerRunning: Boolean,
            val set: ELSet
    ) : WorkoutNotificationState()

    /**
     * Resting between sets. [upNext] is null when the workout has no incomplete sets left.
     */
    data class Rest(
            val remainingSeconds: Int,
            // Percentage of rest time remaining, from 100 down to 0
            val remainingPercent: Int,
            val upNext: NextSet?
    ) : WorkoutNotificationState()

    /**
     * Every set is complete.
     */
    object Done : WorkoutNotificationState()

    /**
     * The workout has no exercises yet.
     */
    object NoExercises : WorkoutNotificationState()

    data class RestTimer(val remainingSeconds: Int, val remainingPercent: Int)

    companion object {

        fun from(workout: ELWorkout?, restTimer: RestTimer?): WorkoutNotificationState {
            val nextSet = nextSet(workout)
            return when {
                restTimer != null -> Rest(restTimer.remainingSeconds, restTimer.remainingPercent, nextSet)
                workout == null || !workout.hasExercises() -> NoExercises
                nextSet == null -> Done
                else -> nextSet
            }
        }

        private fun nextSet(workout: ELWorkout?): NextSet? {
            val state = workout?.getNextIncompleteState() ?: return null
            val group = workout.getExerciseGroups()[state.groupIndex]
            val exercise = group.getExercisesForSetIndex(state.setIndex)[state.exerciseIndex]
            val set = exercise.sets[state.setIndex]
            val timerRunning = set.remainingTimeSeconds != null
            return NextSet(
                    exerciseName = exercise.getName() ?: "",
                    exercisePosition = state.exerciseIndex + 1,
                    exercisesInGroup = state.exercisesInGroup,
                    setType = group.type ?: "",
                    setNumber = state.setIndex + 1,
                    totalSets = group.getTotalSetsCount(),
                    weight = if (set.isWeightEntered()) set.getWeight() else 0f,
                    reps = if (set.canShowRepOptions()) max(0, set.getReps()) else null,
                    timeSeconds = when {
                        !set.canShowTimeOptions() -> null
                        timerRunning -> set.remainingTimeSeconds
                        else -> set.getTimeSeconds()
                    },
                    timerRunning = timerRunning,
                    set = set)
        }
    }
}
