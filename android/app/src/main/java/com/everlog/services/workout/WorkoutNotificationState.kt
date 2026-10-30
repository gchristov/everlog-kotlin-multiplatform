package com.everlog.services.workout

import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.ELWorkoutState
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.toSessionSet
import java.io.Serializable

/**
 * The set the workout screen has selected, which the notification shows, and whether its main button starts the
 * set's timer rather than completing it.
 */
data class ShownSet(val position: ELWorkoutState, val startsTimer: Boolean) : Serializable

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
            // Time rather than reps, as the workout table decides. The values are null when they're empty.
            val timed: Boolean,
            val reps: Int?,
            // A running timer's seconds left
            val timeSeconds: Int?,
            val weightKg: Float?,
            val timerRunning: Boolean,
            val set: ELSet,
            // The main button starts the set's timer rather than completing it
            val startsTimer: Boolean = false
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

        // [shown] is the set to show, the next one not done if it's null
        fun from(workout: ELWorkout?, restTimer: RestTimer?, shown: ShownSet? = null): WorkoutNotificationState {
            val nextSet = nextSet(workout, shown)
            return when {
                restTimer != null -> Rest(restTimer.remainingSeconds, restTimer.remainingPercent, nextSet)
                workout == null || !workout.hasExercises() -> NoExercises
                nextSet == null -> Done
                else -> nextSet
            }
        }

        // A shown set that's no longer in the workout falls back to the next one not done
        private fun nextSet(workout: ELWorkout?, shown: ShownSet?): NextSet? {
            workout ?: return null
            return shown?.let { setAt(workout, it.position)?.copy(startsTimer = it.startsTimer) }
                    ?: workout.getNextIncompleteState()?.let { setAt(workout, it) }
        }

        private fun setAt(workout: ELWorkout, state: ELWorkoutState): NextSet? {
            val group = workout.getExerciseGroups().getOrNull(state.groupIndex) ?: return null
            val exercise = group.getExercisesForSetIndex(state.setIndex).getOrNull(state.exerciseIndex) ?: return null
            val set = exercise.sets.getOrNull(state.setIndex) ?: return null
            val timerRunning = set.remainingTimeSeconds != null
            val values = set.toSessionSet()
            return NextSet(
                    exerciseName = exercise.getName() ?: "",
                    exercisePosition = state.exerciseIndex + 1,
                    exercisesInGroup = state.exercisesInGroup,
                    setType = group.type ?: "",
                    setNumber = state.setIndex + 1,
                    totalSets = group.getTotalSetsCount(),
                    timed = values.mode == SetMode.Time,
                    reps = values.reps,
                    timeSeconds = set.remainingTimeSeconds ?: values.timeSeconds,
                    weightKg = values.weightKg,
                    timerRunning = timerRunning,
                    set = set)
        }
    }
}
