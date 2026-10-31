package com.everlog.android.workout.log

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.ELWorkoutState
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.WorkoutBlock
import com.everlog.android.common.session.withSessionBlocks

// The notification works on ELWorkout, which WorkoutService shows and sends back positions in

/**
 * The set at a position the notification sent: the exercise group, the round, and the exercise's place among those
 * with a set in that round, which skips removed ones.
 */
internal fun ELWorkout.setAt(blocks: List<WorkoutBlock>, position: ELWorkoutState): SetRef? {
    val block = blocks.getOrNull(position.groupIndex) ?: return null
    val round = block.rounds.getOrNull(position.setIndex) ?: return null
    val group = withSessionBlocks(blocks).getExerciseGroups().getOrNull(position.groupIndex) ?: return null
    val exercise = group.getExercisesForSetIndex(position.setIndex).getOrNull(position.exerciseIndex) ?: return null
    val index = group.exercises.indexOfFirst { it === exercise }
    return SetRef(block.id, round.id, index).takeIf { index >= 0 }
}

/**
 * The position of a set as the notification takes it, the inverse of [setAt].
 */
internal fun ELWorkout.positionOf(blocks: List<WorkoutBlock>, ref: SetRef): ELWorkoutState? {
    val groupIndex = blocks.indexOfFirst { it.id == ref.blockId }
    val setIndex = blocks.getOrNull(groupIndex)?.rounds?.indexOfFirst { it.id == ref.roundId } ?: return null
    val group = withSessionBlocks(blocks).getExerciseGroups().getOrNull(groupIndex) ?: return null
    val exercise = group.exercises.getOrNull(ref.index) ?: return null
    val inRound = group.getExercisesForSetIndex(setIndex)
    val exerciseIndex = inRound.indexOfFirst { it === exercise }
    if (setIndex < 0 || exerciseIndex < 0) return null
    return ELWorkoutState(groupIndex, setIndex, exerciseIndex, inRound.size)
}

/**
 * The workout as the notification shows it, with a running set timer's seconds left on its set.
 */
internal fun ELWorkout.forNotification(state: WorkoutLogState): ELWorkout {
    val workout = withSessionBlocks(state.blocks)
    val timer = state.timer as? WorkoutTimer.SetTimer ?: return workout
    val blockIndex = state.blocks.indexOfFirst { it.id == timer.ref.blockId }
    val roundIndex = state.blocks.getOrNull(blockIndex)?.rounds?.indexOfFirst { it.id == timer.ref.roundId } ?: -1
    workout.getExerciseGroups().getOrNull(blockIndex)?.exercises?.getOrNull(timer.ref.index)?.sets?.getOrNull(roundIndex)
        ?.remainingTimeSeconds = state.timerRemainingSeconds()
    return workout
}
