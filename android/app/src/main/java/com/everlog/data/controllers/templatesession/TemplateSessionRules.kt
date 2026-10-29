package com.everlog.data.controllers.templatesession

import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutsession.ExerciseRef
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.set.ELSetType
import java.util.UUID

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

/**
 * Makes the [picked] exercises one block of [type], where the first of them was. They keep their sets, in the
 * table's order; one with fewer sets than the others gets copies of its last, so the rounds line up. The blocks they
 * came from keep their other exercises, as a single when one is left. Picking the whole of one block only changes
 * its type.
 */
fun List<WorkoutBlock>.groupExercises(picked: Collection<ExerciseRef>, type: ELSetType, ids: SessionIds): List<WorkoutBlock> {
    val ordered = ordered(picked).ifEmpty { return this }
    val first = block(ordered.first().blockId) ?: return this
    if (ordered.all { it.blockId == first.id } && ordered.size == first.exercises.size) {
        return map { if (it.id == first.id) it.copy(type = type.name) else it }
    }
    val sets = ordered.map { ref -> exerciseSets(ref) }
    val rounds = sets.maxOf { it.size }.coerceAtLeast(1)
    val grouped = WorkoutBlock(
        id = ids.next(),
        uuid = UUID.randomUUID().toString(),
        type = type.name,
        restSeconds = first.restSeconds,
        exercises = ordered.map { ref -> block(ref.blockId)!!.exercises[ref.index] },
        rounds = List(rounds) { r ->
            WorkoutRound(ids.next(), sets.map { exercise -> exercise.getOrNull(r) ?: exercise.lastOrNull()?.asCopy() ?: WorkoutSet() })
        },
    )
    return flatMap { block ->
        val rest = block.without(ordered.filter { it.blockId == block.id }.map { it.index }.toSet())
        if (block.id == first.id) listOfNotNull(grouped, rest) else listOfNotNull(rest)
    }
}

/**
 * Takes the [picked] exercises out of their supersets and circuits, each as a single right after the group it left.
 * A group left with one exercise becomes a single. Picked exercises that are singles already stay as they are.
 */
fun List<WorkoutBlock>.ungroupExercises(picked: Collection<ExerciseRef>, ids: SessionIds): List<WorkoutBlock> = flatMap { block ->
    val out = picked.filter { it.blockId == block.id }.map { it.index }.sorted()
    if (block.exercises.size < 2 || out.isEmpty()) return@flatMap listOf(block)
    val singles = out.map { index ->
        WorkoutBlock(
            id = ids.next(),
            uuid = UUID.randomUUID().toString(),
            type = ELSetType.SINGLE.name,
            restSeconds = block.restSeconds,
            exercises = listOf(block.exercises[index]),
            rounds = block.rounds.map { it.sets[index] }.filterNot { it.removed }.map { WorkoutRound(ids.next(), listOf(it)) },
        )
    }.filter { it.rounds.isNotEmpty() }
    listOfNotNull(block.without(out.toSet())) + singles
}

// In the table's order: block by block, exercise by exercise
private fun List<WorkoutBlock>.ordered(picked: Collection<ExerciseRef>): List<ExerciseRef> {
    val position = mapIndexed { i, block -> block.id to i }.toMap()
    return picked.filter { ref -> block(ref.blockId)?.exercises?.indices?.contains(ref.index) == true }
        .sortedWith(compareBy({ position[it.blockId] }, { it.index }))
}

private fun List<WorkoutBlock>.exerciseSets(ref: ExerciseRef): List<WorkoutSet> =
    block(ref.blockId)?.rounds?.map { it.sets[ref.index] }?.filterNot { it.removed }.orEmpty()

private fun WorkoutSet.asCopy() = copy(startedDate = null, completedDate = null, removed = false)

// Without the exercises at [indexes]: a single when one is left, null when none is
private fun WorkoutBlock.without(indexes: Set<Int>): WorkoutBlock? {
    if (indexes.isEmpty()) return this
    val keep = exercises.indices.filterNot { it in indexes }
    val rounds = rounds.map { round -> round.copy(sets = keep.map { round.sets[it] }) }.filterNot { round -> round.sets.all { it.removed } }
    if (keep.isEmpty() || rounds.isEmpty()) return null
    return copy(
        exercises = keep.map { exercises[it] },
        rounds = rounds,
        type = if (keep.size == 1) ELSetType.SINGLE.name else type,
    )
}
