package com.everlog.data.controllers.templatesession

import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.toSessionBlock
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import org.apache.commons.lang3.SerializationUtils

// Maps a routine (a workout template) between ELRoutine and the session's blocks, so the template editor can use the
// workout screen's model, table and rules. A template's sets hold targets only, which ELRoutine stores as requiredReps
// or requiredTimeSeconds. In the blocks they sit in the sets' reps and time, the values the table shows and the
// keypad types into. Templates hold no weight.

fun ELRoutine.toTemplateBlocks(ids: SessionIds): List<WorkoutBlock> = exerciseGroups.map { it.toTemplateBlock(ids) }

fun ELExerciseGroup.toTemplateBlock(ids: SessionIds): WorkoutBlock {
    val block = toSessionBlock(ids)
    return block.copy(rounds = block.rounds.map { round -> round.copy(sets = round.sets.map { it.asTarget() }) })
}

private fun WorkoutSet.asTarget() = WorkoutSet(mode = mode, reps = requiredReps, timeSeconds = requiredTimeSeconds, removed = removed)

/**
 * The rest after each set. It's one time for the whole template, which every group holds a copy of.
 */
fun ELRoutine.templateRestSeconds(): Int = exerciseGroups.firstOrNull()?.restTimeSeconds ?: AppConfig.configuration.defaultRestTimeSeconds

/**
 * A copy of this routine with the editor's [blocks] as its exercise groups, each with [restSeconds] of rest. Removed
 * sets are left out, and so is anything left empty without them.
 */
fun ELRoutine.withTemplateBlocks(blocks: List<WorkoutBlock>, restSeconds: Int): ELRoutine {
    val copy = SerializationUtils.clone(this)
    copy.exerciseGroups = blocks.mapNotNull { it.toTemplateGroup(restSeconds) }.toMutableList()
    return copy
}

private fun WorkoutBlock.toTemplateGroup(restSeconds: Int): ELExerciseGroup? {
    val exercises = exercises.mapIndexedNotNull { index, exercise ->
        val sets = rounds.map { it.sets[index] }.filterNot { it.removed }
        if (sets.isEmpty()) return@mapIndexedNotNull null
        ELRoutineExercise(exercise.uuid, exercise.exercise, sets.map { it.toTargetSet() }.toMutableList())
    }
    if (exercises.isEmpty()) return null
    val type = if (exercises.size == 1 && this.exercises.size > 1) ELSetType.SINGLE.name else type
    return ELExerciseGroup(uuid = uuid, type = type, exercises = exercises.toMutableList(), restTimeSeconds = restSeconds)
}

// Reps and time are separate in ELSet, so only the mode's target is saved
private fun WorkoutSet.toTargetSet() = ELSet(
    requiredReps = reps.takeIf { mode == SetMode.Reps },
    requiredTimeSeconds = timeSeconds.takeIf { mode == SetMode.Time },
)
