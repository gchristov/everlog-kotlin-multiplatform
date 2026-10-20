package com.everlog.data.controllers.workoutsession

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSetType

// The ongoing workout as the new workout screen (TAS-455) holds it: immutable, so a view-model's state can hold
// it, and mapped to and from ELWorkout (WorkoutSessionMapper), which stays what's saved, sent to the notification
// and stored in Firestore. The rules that change it are in WorkoutSessionRules.

enum class SetMode {
    Reps,
    Time,
}

enum class ValueField {
    Reps,
    Time,
    Weight,
}

/**
 * One exercise's set. It records weight plus either reps or time, whichever [mode] says. Both are kept while the
 * workout is open, so switching back and forth doesn't lose a value, but only [mode]'s is saved. The other fields
 * are ELSet's, carried as they are.
 */
data class WorkoutSet(
    val mode: SetMode = SetMode.Reps,
    val reps: Int? = null,
    val timeSeconds: Int? = null,
    val weightKg: Float? = null,
    // The routine's targets
    val requiredReps: Int? = null,
    val requiredTimeSeconds: Int? = null,
    val startedDate: Long? = null,
    val completedDate: Long? = null,
    // Deleted from one exercise in a superset or circuit's round, which keeps it so each set still lines up
    // with its exercise. It's left out of everything; once every exercise's set is gone, the round goes.
    val removed: Boolean = false,
    // The weight filled in from history, in kg, while it's still the set's weight
    val suggestedKg: Float? = null,
) {
    val done: Boolean
        get() = completedDate != null

    // The weight is the one filled in from history, not changed since, and the set isn't done
    val weightSuggested: Boolean
        get() = !done && suggestedKg != null && suggestedKg == weightKg
}

/**
 * An exercise in a block. [uuid] is the routine exercise's, [exercise] the library or user's exercise.
 */
data class WorkoutExercise(
    val uuid: String?,
    val exercise: ELExercise?,
)

/**
 * A round of a block: one set of each of its exercises, in the same order. For a single exercise it's just its set.
 */
data class WorkoutRound(
    val id: Long,
    val sets: List<WorkoutSet>,
) {
    // Every exercise's set that's still there is done
    val done: Boolean
        get() = sets.all { it.done || it.removed }
}

/**
 * An exercise group: one exercise, or exercises done together (a superset, circuit and so on, by [type]).
 */
data class WorkoutBlock(
    val id: Long,
    val uuid: String?,
    // ELSetType's name, e.g. "SUPER"
    val type: String?,
    val restSeconds: Int,
    val exercises: List<WorkoutExercise>,
    val rounds: List<WorkoutRound>,
) {
    // Null for a type this version doesn't know
    val setType: ELSetType?
        get() = type?.let { name -> ELSetType.entries.firstOrNull { it.name == name } }
}

/**
 * One exercise's set ([index] is the exercise's) in one round of a block.
 */
data class SetRef(
    val blockId: Long,
    val roundId: Long,
    val index: Int,
)

/**
 * A block as it was before some of its sets were deleted, and where it was, so the delete can be undone.
 */
data class DeletedSets(
    val block: WorkoutBlock,
    val position: Int,
)

/**
 * Ids for blocks and rounds, unique within one screen. They aren't saved: a workout loaded again gets new ones.
 */
class SessionIds(start: Long = 0) {
    private var last = start

    fun next(): Long = ++last
}
