package com.everlog.data.controllers.templatesession

import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExerciseGroup

// How the template editor changes a template's blocks. Adding rounds, deleting sets and moving exercises are the
// workout's rules (WorkoutSessionRules), which work the same on targets.

const val NewExerciseSets = 3
const val DefaultReps = 10
val DefaultTimeSeconds = AppConfig.configuration.defaultExerciseTimeSeconds

/**
 * Exercises picked for a template, which start with [NewExerciseSets] sets of [DefaultReps]. A group that already
 * has targets keeps them.
 */
fun List<WorkoutBlock>.addTemplateGroups(groups: List<ELExerciseGroup>, ids: SessionIds): List<WorkoutBlock> =
    this + groups.map { group ->
        val block = group.toTemplateBlock(ids)
        val empty = block.rounds.all { round -> round.sets.all { it.reps == null && it.timeSeconds == null } }
        if (!empty) return@map block
        block.copy(rounds = List(NewExerciseSets) { WorkoutRound(ids.next(), block.exercises.map { WorkoutSet(reps = DefaultReps) }) })
    }

/**
 * Switches every set of one of a block's exercises to reps or time: in a template it's the exercise that's done for
 * reps or for time, not each set. A set keeps a value it had of that kind, and otherwise starts at [DefaultReps] or
 * [DefaultTimeSeconds].
 */
fun List<WorkoutBlock>.setExerciseMode(blockId: Long, exercise: Int, mode: SetMode): List<WorkoutBlock> =
    updateExerciseSets(blockId, exercise) { set ->
        when (mode) {
            SetMode.Reps -> set.copy(mode = mode, reps = set.reps ?: DefaultReps)
            SetMode.Time -> set.copy(mode = mode, timeSeconds = set.timeSeconds ?: DefaultTimeSeconds)
        }
    }

/**
 * Gives every set of one of a block's exercises the same time.
 */
fun List<WorkoutBlock>.withTimeForAllSets(blockId: Long, exercise: Int, seconds: Int): List<WorkoutBlock> =
    updateExerciseSets(blockId, exercise) { it.copy(mode = SetMode.Time, timeSeconds = seconds.takeIf { s -> s > 0 }) }

private fun List<WorkoutBlock>.updateExerciseSets(blockId: Long, exercise: Int, transform: (WorkoutSet) -> WorkoutSet): List<WorkoutBlock> =
    map { block ->
        if (block.id != blockId) return@map block
        block.copy(rounds = block.rounds.map { round ->
            round.copy(sets = round.sets.mapIndexed { i, set -> if (i == exercise && !set.removed) transform(set) else set })
        })
    }
