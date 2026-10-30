package com.everlog.android.common.data.model.exercise

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.estimate1RM

// 1RM formulas are only reliable up to ~10 reps, beyond that they wildly overestimate
private const val ORM_MAX_REPS = 10

/**
 * The exercise's 1RM in [unit], estimated from its heaviest set that has weight and at most [ORM_MAX_REPS] reps,
 * or 0 without such a set.
 */
fun ELRoutineExercise.estimated1RM(unit: WeightUnit): Float {
    val heaviestSet = sets
        .filter { it.isWeightEntered() && it.getReps() in 1..ORM_MAX_REPS }
        .maxByOrNull { it.getWeight(unit) }
        ?: return 0f
    return estimate1RM(heaviestSet.getWeight(unit), heaviestSet.getReps())
}
