package com.everlog.android.common.kotlin.format

import android.content.Context
import com.everlog.android.common.kotlin.R

// By the set type's name (ELSetType in common:data, which checks every type has both)
private val titles = mapOf(
    "SINGLE" to R.string.set_single,
    "DROP" to R.string.set_drop,
    "SUPER" to R.string.set_super,
    "GIANT" to R.string.set_giant,
    "FORCE" to R.string.set_force,
    "CIRCUIT" to R.string.set_circuit,
    "PROGRESSIVE" to R.string.set_progressive,
    "MULTI" to R.string.set_multi,
    "COMBO" to R.string.set_combo,
    "AMRAP" to R.string.set_amrap,
)

private val descriptions = mapOf(
    "SINGLE" to R.string.set_single_description,
    "DROP" to R.string.set_drop_description,
    "SUPER" to R.string.set_super_description,
    "GIANT" to R.string.set_giant_description,
    "FORCE" to R.string.set_force_description,
    "CIRCUIT" to R.string.set_circuit_description,
    "PROGRESSIVE" to R.string.set_progressive_description,
    "MULTI" to R.string.set_multi_description,
    "COMBO" to R.string.set_combo_description,
    "AMRAP" to R.string.set_amrap_description,
)

fun setTypeTitleRes(name: String): Int? = titles[name]

fun setTypeDescriptionRes(name: String): Int? = descriptions[name]

fun setTypeTitle(context: Context, name: String): String? = setTypeTitleRes(name)?.let(context::getString)

fun setTypeDescription(context: Context, name: String): String? = setTypeDescriptionRes(name)?.let(context::getString)
