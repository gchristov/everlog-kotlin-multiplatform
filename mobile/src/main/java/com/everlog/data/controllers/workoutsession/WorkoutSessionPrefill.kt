package com.everlog.data.controllers.workoutsession

import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.controllers.workoutprefill.WorkoutPrefillController
import com.everlog.data.model.set.ELSet
import com.everlog.managers.preferences.SettingsManager

/**
 * Fills sets in from the user's history: the 1RM target for the muscle goal in Settings, or else each set's value
 * from the exercise's last session.
 */
interface WorkoutSetPrefill {
    // Whether the muscle goal fills sets in at all
    fun fillsSets(): Boolean

    // Fills the set's empty values. [index] is the set's place in the exercise.
    fun fill(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet

    // A set just added copied the last set's values: the last session's set at its place replaces them, if there
    // was one
    fun fillAdded(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet
}

class RealWorkoutSetPrefill(
    private val fillsSets: () -> Boolean = { SettingsManager.manager.muscleGoal().canPrefill() },
) : WorkoutSetPrefill {
    override fun fillsSets(): Boolean = fillsSets.invoke()

    override fun fill(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet =
        set.throughELSet { WorkoutPrefillController.prefillSet(source, it, index) }

    override fun fillAdded(source: PrefillSource, set: WorkoutSet, index: Int): WorkoutSet =
        set.throughELSet { WorkoutPrefillController.prefillAddedSet(source, it, index) }

    // WorkoutPrefillController fills an ELSet in place
    private fun WorkoutSet.throughELSet(fill: (ELSet) -> Unit): WorkoutSet {
        val set = toELSet()
        fill(set)
        return set.toSessionSet()
    }
}

/**
 * Every set not done gets its empty values filled in from its exercise's history, when the muscle goal fills sets in.
 */
fun WorkoutBlock.filledFromHistory(sources: Map<String, PrefillSource>, prefill: WorkoutSetPrefill): WorkoutBlock {
    if (!prefill.fillsSets()) return this
    return copy(rounds = rounds.mapIndexed { index, round ->
        round.copy(sets = round.sets.mapIndexed { i, set ->
            val source = sources[exercises[i].exercise?.uuid]
            if (set.removed || set.done || source == null) set else set.suggesting(prefill.fill(source, set, index))
        })
    })
}

/**
 * Fills the block in as it was when history started loading ([before]). Sets changed since, e.g. a weight cleared,
 * stay as they are now.
 */
fun WorkoutBlock.filledFromHistory(before: WorkoutBlock, sources: Map<String, PrefillSource>, prefill: WorkoutSetPrefill): WorkoutBlock {
    val filled = before.filledFromHistory(sources, prefill)
    return copy(rounds = rounds.map { round ->
        val was = before.rounds.firstOrNull { it.id == round.id }
        val fill = filled.rounds.firstOrNull { it.id == round.id }
        // A round whose exercises changed since keeps its sets
        if (was == null || fill == null || was.sets.size != round.sets.size) return@map round
        round.copy(sets = round.sets.mapIndexed { i, set -> if (set == was.sets[i]) fill.sets[i] else set })
    })
}

/**
 * The block's last round, just added, filled in from the last session's sets at its place.
 */
fun WorkoutBlock.withAddedRoundFilled(sources: Map<String, PrefillSource>, prefill: WorkoutSetPrefill): WorkoutBlock {
    if (!prefill.fillsSets()) return this
    val index = rounds.lastIndex
    val round = rounds.lastOrNull() ?: return this
    val filled = round.copy(sets = round.sets.mapIndexed { i, set ->
        val source = sources[exercises[i].exercise?.uuid] ?: return@mapIndexed set
        set.suggesting(prefill.fillAdded(source, set, index))
    })
    return copy(rounds = rounds.dropLast(1) + filled)
}

fun PrefillSource.lastSets(): List<WorkoutSet> = lastSession?.exercise?.sets.orEmpty().map { it.toSessionSet() }

// A value the history changed is the set's suggestion
private fun WorkoutSet.suggesting(filled: WorkoutSet): WorkoutSet = filled.copy(
    suggestedKg = filled.weightKg?.takeIf { it != weightKg } ?: suggestedKg,
    suggestedReps = filled.reps?.takeIf { it != reps } ?: suggestedReps,
    suggestedTimeSeconds = filled.timeSeconds?.takeIf { it != timeSeconds } ?: suggestedTimeSeconds,
)
