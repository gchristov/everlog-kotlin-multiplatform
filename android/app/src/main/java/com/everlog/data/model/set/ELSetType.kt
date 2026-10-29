package com.everlog.data.model.set

import android.content.Context
import com.everlog.R

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

    fun getTitle(context: Context): String? {
        return when (this) {
            SINGLE -> context.getString(R.string.set_single)
            DROP -> context.getString(R.string.set_drop)
            SUPER -> context.getString(R.string.set_super)
            GIANT -> context.getString(R.string.set_giant)
            FORCE -> context.getString(R.string.set_force)
            CIRCUIT -> context.getString(R.string.set_circuit)
            PROGRESSIVE -> context.getString(R.string.set_progressive)
            MULTI -> context.getString(R.string.set_multi)
            COMBO -> context.getString(R.string.set_combo)
            AMRAP -> context.getString(R.string.set_amrap)
        }
    }

    fun getDescription(context: Context): String? {
        return when (this) {
            SINGLE -> context.getString(R.string.set_single_description)
            DROP -> context.getString(R.string.set_drop_description)
            SUPER -> context.getString(R.string.set_super_description)
            GIANT -> context.getString(R.string.set_giant_description)
            FORCE -> context.getString(R.string.set_force_description)
            CIRCUIT -> context.getString(R.string.set_circuit_description)
            PROGRESSIVE -> context.getString(R.string.set_progressive_description)
            MULTI -> context.getString(R.string.set_multi_description)
            COMBO -> context.getString(R.string.set_combo_description)
            AMRAP -> context.getString(R.string.set_amrap_description)
        }
    }
}