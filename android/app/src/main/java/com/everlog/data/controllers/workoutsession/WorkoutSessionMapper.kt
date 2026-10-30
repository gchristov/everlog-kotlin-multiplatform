package com.everlog.data.controllers.workoutsession

import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import org.apache.commons.lang3.SerializationUtils

// Maps the ongoing workout between ELWorkout and the session's blocks. A round is the sets at the same index
// across a group's exercises. An exercise with fewer sets than the others has removed sets in the rounds it's
// missing from, so the rounds still line up.

fun ELWorkout.toSessionBlocks(ids: SessionIds): List<WorkoutBlock> {
    return getExerciseGroups().map { it.toSessionBlock(ids) }
}

fun ELExerciseGroup.toSessionBlock(ids: SessionIds): WorkoutBlock {
    return WorkoutBlock(
        id = ids.next(),
        uuid = uuid,
        type = type,
        restSeconds = restTimeSeconds,
        exercises = exercises.map { WorkoutExercise(it.uuid, it.exercise) },
        rounds = (0 until getTotalSetsCount()).map { index ->
            WorkoutRound(ids.next(), exercises.map { it.sets.getOrNull(index)?.toSessionSet() ?: WorkoutSet(removed = true) })
        },
    )
}

/**
 * A copy of this workout with the session's [blocks] as its exercise groups.
 *
 * @param finished Leaves out removed sets and anything left empty without them, for the workout that's saved to
 * Firestore. The ongoing workout keeps them, so its rounds line up when it's loaded again.
 */
fun ELWorkout.withSessionBlocks(blocks: List<WorkoutBlock>, finished: Boolean = false): ELWorkout {
    val copy = SerializationUtils.clone(this)
    copy.setExerciseGroups(blocks.mapNotNull { it.toExerciseGroup(finished) }.toMutableList())
    return copy
}

private fun WorkoutBlock.toExerciseGroup(finished: Boolean): ELExerciseGroup? {
    val exercises = exercises.mapIndexedNotNull { index, exercise ->
        val sets = rounds.map { it.sets[index] }
            // Removed sets after the exercise's last set aren't needed to line it up
            .dropLastWhile { it.removed }
            .filterNot { finished && it.removed }
        if (finished && sets.isEmpty()) return@mapIndexedNotNull null
        ELRoutineExercise(exercise.uuid, exercise.exercise, sets.map { it.toELSet() }.toMutableList())
    }
    if (exercises.isEmpty()) {
        return null
    }
    // The rules take an exercise with no sets left out of its block already, this is in case one slips through
    val type = if (exercises.size == 1 && exercises.size < this.exercises.size) ELSetType.SINGLE.name else type
    return ELExerciseGroup(uuid = uuid, type = type, exercises = exercises.toMutableList(), restTimeSeconds = restSeconds)
}

internal fun ELSet.toSessionSet(): WorkoutSet {
    val requiredTime = if (isRequiredTimeEntered()) getRequiredTimeSeconds() else null
    val time = if (isTimeEntered()) getTimeSeconds() else null
    return WorkoutSet(
        // Time when there's a time, or only a target time
        mode = if (time != null || (!isRepsEntered() && requiredTime != null)) SetMode.Time else SetMode.Reps,
        reps = getReps().takeIf { it > 0 },
        timeSeconds = time,
        // Older app versions saved a missing weight as -1
        weightKg = getWeightKg()?.takeIf { it > 0 },
        requiredReps = getRequiredReps().takeIf { it > 0 },
        requiredTimeSeconds = requiredTime,
        startedDate = getStartedDate().takeIf { it > 0 },
        completedDate = getCompletedDate().takeIf { it > 0 },
        removed = removed,
        suggestedKg = suggestedWeight,
        suggestedReps = suggestedReps,
        suggestedTimeSeconds = suggestedTimeSeconds,
    )
}

// Reps and time are separate in ELSet, so only the mode's are saved
internal fun WorkoutSet.toELSet(): ELSet {
    val reps = mode == SetMode.Reps
    return ELSet(
        startedDate = startedDate,
        completedDate = completedDate,
        requiredReps = requiredReps.takeIf { reps },
        reps = this.reps.takeIf { reps },
        weight = weightKg,
        requiredTimeSeconds = requiredTimeSeconds.takeUnless { reps },
        timeSeconds = timeSeconds.takeUnless { reps },
        removed = removed,
        suggestedWeight = suggestedKg,
        suggestedReps = suggestedReps.takeIf { reps },
        suggestedTimeSeconds = suggestedTimeSeconds.takeUnless { reps },
    )
}
