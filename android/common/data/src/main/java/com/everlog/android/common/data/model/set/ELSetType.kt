package com.everlog.android.common.data.model.set


enum class ELSetType {

    SINGLE,
    DROP,
    SUPER,
    GIANT,
    FORCE,
    CIRCUIT,
    PROGRESSIVE,
    MULTI,
    COMBO,
    AMRAP;

    fun canBeSelected(selectedExercisesCount: Int): Boolean {
        return minAllowedExercises() <= selectedExercisesCount && selectedExercisesCount <= maxAllowedExercises()
    }

    fun maxAllowedExercises(): Int {
        return when (this) {
            SINGLE, DROP, FORCE, PROGRESSIVE, AMRAP -> 1
            SUPER -> 2
            else -> Int.MAX_VALUE
        }
    }

    fun minAllowedExercises(): Int {
        return when (this) {
            SUPER, COMBO, CIRCUIT -> 2
            GIANT -> 3
            else -> 1
        }
    }

}