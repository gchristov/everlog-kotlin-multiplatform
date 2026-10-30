package com.everlog.data.controllers.workoutsession

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.set.ELSetType

// How the ongoing workout changes, as pure functions on its blocks, so they can be tested without a screen.
// Sets are done in order: block by block, round by round, exercise by exercise. In a superset or circuit a round
// isn't left until all its exercises are done, and rest comes after the round.

/**
 * Every set that's still there, in the order they're done.
 */
fun List<WorkoutBlock>.refs(): List<SetRef> = flatMap { block ->
    block.rounds.flatMap { round -> round.sets.indices.filter { !round.sets[it].removed }.map { SetRef(block.id, round.id, it) } }
}

fun List<WorkoutBlock>.block(id: Long): WorkoutBlock? = firstOrNull { it.id == id }

fun List<WorkoutBlock>.round(blockId: Long, roundId: Long): WorkoutRound? = block(blockId)?.rounds?.firstOrNull { it.id == roundId }

fun List<WorkoutBlock>.set(ref: SetRef): WorkoutSet? = round(ref.blockId, ref.roundId)?.sets?.getOrNull(ref.index)

/**
 * The first set not done after [ref], going round to the start if there's none after it. The first one not done
 * when [ref] is null.
 */
fun List<WorkoutBlock>.nextOpenAfter(ref: SetRef?): SetRef? {
    val refs = refs()
    val start = refs.indexOf(ref)
    val ordered = refs.drop(start + 1) + refs.take(start + 1)
    return ordered.firstOrNull { set(it)?.done == false }
}

fun List<WorkoutBlock>.unfinishedSets(): Int = refs().count { set(it)?.done == false }

fun List<WorkoutBlock>.updateSet(ref: SetRef, transform: (WorkoutSet) -> WorkoutSet): List<WorkoutBlock> =
    updateRound(ref.blockId, ref.roundId) { round -> round.copy(sets = round.sets.mapIndexed { i, set -> if (i == ref.index) transform(set) else set }) }

private fun List<WorkoutBlock>.updateRound(blockId: Long, roundId: Long, transform: (WorkoutRound) -> WorkoutRound): List<WorkoutBlock> = map { block ->
    if (block.id != blockId) block else block.copy(rounds = block.rounds.map { if (it.id == roundId) transform(it) else it })
}

/**
 * Sets one of the set's values. Like ELSet, nothing under 1 counts as a value, so it clears it.
 *
 * @param value Reps, seconds, or kg for [ValueField.Weight]
 */
fun List<WorkoutBlock>.setValue(ref: SetRef, field: ValueField, value: Float?): List<WorkoutBlock> = updateSet(ref) { set ->
    val entered = value?.takeIf { it > 0 }
    when (field) {
        ValueField.Reps -> set.copy(reps = entered?.toInt()?.takeIf { it > 0 })
        ValueField.Time -> set.copy(timeSeconds = entered?.toInt()?.takeIf { it > 0 })
        ValueField.Weight -> set.copy(weightKg = entered)
    }
}

fun List<WorkoutBlock>.setMode(ref: SetRef, mode: SetMode): List<WorkoutBlock> = updateSet(ref) { it.copy(mode = mode) }

// A set's start date is when it was completed: nothing records when a set starts
private fun WorkoutSet.completed(now: Long) = copy(startedDate = now, completedDate = now)

/**
 * The result of completing a set: the blocks, the set that's current after it, and whether rest starts.
 */
data class Completion(
    val blocks: List<WorkoutBlock>,
    val current: SetRef?,
    val startsRest: Boolean,
)

/**
 * Completes the current set and moves on. In a superset or circuit, a round with exercises still to do goes back
 * to the earliest of them, without rest. Once the round is done it moves on to the next set not done, and rest
 * starts, unless that was the last set not done in the workout.
 */
fun List<WorkoutBlock>.completeCurrent(current: SetRef, now: Long): Completion {
    val blocks = updateSet(current) { it.completed(now) }
    val round = blocks.round(current.blockId, current.roundId)
    val leftInRound = round?.sets?.indexOfFirst { !it.done && !it.removed }?.takeIf { it >= 0 }
    if (leftInRound != null) {
        return Completion(blocks, SetRef(current.blockId, current.roundId, leftInRound), startsRest = false)
    }
    val next = blocks.nextOpenAfter(current)
    return Completion(blocks, next, startsRest = next != null)
}

/**
 * Completes one exercise's set that may not be the current one. The current set moves on as [completeCurrent]
 * does; for another, it stays (or becomes the next one not done, if there was none) and rest starts once the
 * set's round is done.
 */
fun List<WorkoutBlock>.completeSet(ref: SetRef, current: SetRef?, now: Long): Completion {
    if (set(ref)?.done != false) return Completion(this, current, startsRest = false)
    if (ref == current) return completeCurrent(ref, now)
    val blocks = updateSet(ref) { it.completed(now) }
    val next = current ?: blocks.nextOpenAfter(ref)
    val roundDone = blocks.round(ref.blockId, ref.roundId)?.done == true
    return Completion(blocks, next, startsRest = roundDone && next != null)
}

/**
 * Keeps the set's values and start date.
 */
fun List<WorkoutBlock>.uncompleteSet(ref: SetRef): List<WorkoutBlock> = updateSet(ref) { it.copy(completedDate = null) }

fun List<WorkoutBlock>.addGroups(groups: List<ELExerciseGroup>, ids: SessionIds): List<WorkoutBlock> = this + groups.map { it.toSessionBlock(ids) }

/**
 * A new round after the last, with the last round's values and targets, not done. Every exercise gets a set, one
 * removed from the last round included.
 */
fun List<WorkoutBlock>.addRound(blockId: Long, ids: SessionIds): List<WorkoutBlock> = map { block ->
    val last = block.rounds.lastOrNull()
    if (block.id != blockId || last == null) return@map block
    val sets = last.sets.map { it.copy(startedDate = null, completedDate = null, removed = false) }
    block.copy(rounds = block.rounds + WorkoutRound(ids.next(), sets))
}

/**
 * Deletes a round, and its block with it if it was the last. Returns null if there's no such round, otherwise the
 * blocks and what to put back to undo it.
 */
fun List<WorkoutBlock>.deleteRound(blockId: Long, roundId: Long): Pair<List<WorkoutBlock>, DeletedSets>? {
    val block = block(blockId) ?: return null
    if (block.rounds.none { it.id == roundId }) return null
    return replace(block.copy(rounds = block.rounds.filterNot { it.id == roundId })) to DeletedSets(block, indexOf(block))
}

/**
 * Deletes one exercise's set. In a superset or circuit only that set goes, unless it was the last one left in its
 * round, which then goes too. A single exercise's set is its round. An exercise with no sets left goes from the
 * block, and a block left with one exercise becomes a single.
 */
fun List<WorkoutBlock>.deleteSet(ref: SetRef): Pair<List<WorkoutBlock>, DeletedSets>? {
    val block = block(ref.blockId) ?: return null
    val round = block.rounds.firstOrNull { it.id == ref.roundId } ?: return null
    val othersLeft = round.sets.withIndex().any { (i, set) -> i != ref.index && !set.removed }
    if (!othersLeft) return deleteRound(ref.blockId, ref.roundId)
    val removed = block.copy(rounds = block.rounds.map { r ->
        if (r.id != ref.roundId) r else r.copy(sets = r.sets.mapIndexed { i, set -> if (i == ref.index) set.copy(removed = true) else set })
    })
    val exerciseLeft = removed.rounds.any { !it.sets[ref.index].removed }
    return replace(if (exerciseLeft) removed else removed.withoutExercise(ref.index)) to DeletedSets(block, indexOf(block))
}

private fun WorkoutBlock.withoutExercise(index: Int): WorkoutBlock {
    val exercises = exercises.filterIndexed { i, _ -> i != index }
    return copy(
        exercises = exercises,
        rounds = rounds.map { round -> round.copy(sets = round.sets.filterIndexed { i, _ -> i != index }) }.filterNot { round -> round.sets.all { it.removed } },
        type = if (exercises.size == 1) ELSetType.SINGLE.name else type,
    )
}

// A block left without sets goes
private fun List<WorkoutBlock>.replace(block: WorkoutBlock): List<WorkoutBlock> =
    if (block.rounds.isEmpty()) filterNot { it.id == block.id } else map { if (it.id == block.id) block else it }

/**
 * Puts a block back as it was before its sets were deleted, where it was if it went. Changes made to the block since
 * are lost too, so the undo should go once anything else changes.
 */
fun List<WorkoutBlock>.restore(deleted: DeletedSets): List<WorkoutBlock> =
    if (any { it.id == deleted.block.id }) {
        replace(deleted.block)
    } else {
        toMutableList().apply { add(deleted.position.coerceIn(0, size), deleted.block) }
    }

fun List<WorkoutBlock>.deleteBlock(blockId: Long): List<WorkoutBlock> = filterNot { it.id == blockId }

fun List<WorkoutBlock>.moveBlock(from: Int, to: Int): List<WorkoutBlock> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

fun List<WorkoutBlock>.withRestSeconds(seconds: Int): List<WorkoutBlock> = map { it.copy(restSeconds = seconds) }

/**
 * The exercises as they are now, e.g. renamed since the workout started, by uuid. Exercises not in [current] stay as
 * they are, and equal blocks come back when nothing differs.
 */
fun List<WorkoutBlock>.withExercises(current: Map<String, ELExercise>): List<WorkoutBlock> = map { block ->
    block.copy(exercises = block.exercises.map { item -> current[item.exercise?.uuid]?.let { item.copy(exercise = it) } ?: item })
}
