package com.everlog.utils.format

import android.content.Context
import com.everlog.R
import com.everlog.data.model.set.ELSetType

fun ELSetType.getTitle(context: Context): String? {
    return when (this) {
        ELSetType.SINGLE -> context.getString(R.string.set_single)
        ELSetType.DROP -> context.getString(R.string.set_drop)
        ELSetType.SUPER -> context.getString(R.string.set_super)
        ELSetType.GIANT -> context.getString(R.string.set_giant)
        ELSetType.FORCE -> context.getString(R.string.set_force)
        ELSetType.CIRCUIT -> context.getString(R.string.set_circuit)
        ELSetType.PROGRESSIVE -> context.getString(R.string.set_progressive)
        ELSetType.MULTI -> context.getString(R.string.set_multi)
        ELSetType.COMBO -> context.getString(R.string.set_combo)
        ELSetType.AMRAP -> context.getString(R.string.set_amrap)
    }
}

fun ELSetType.getDescription(context: Context): String? {
    return when (this) {
        ELSetType.SINGLE -> context.getString(R.string.set_single_description)
        ELSetType.DROP -> context.getString(R.string.set_drop_description)
        ELSetType.SUPER -> context.getString(R.string.set_super_description)
        ELSetType.GIANT -> context.getString(R.string.set_giant_description)
        ELSetType.FORCE -> context.getString(R.string.set_force_description)
        ELSetType.CIRCUIT -> context.getString(R.string.set_circuit_description)
        ELSetType.PROGRESSIVE -> context.getString(R.string.set_progressive_description)
        ELSetType.MULTI -> context.getString(R.string.set_multi_description)
        ELSetType.COMBO -> context.getString(R.string.set_combo_description)
        ELSetType.AMRAP -> context.getString(R.string.set_amrap_description)
    }
}
