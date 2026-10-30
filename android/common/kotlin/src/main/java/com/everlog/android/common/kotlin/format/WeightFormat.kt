package com.everlog.android.common.kotlin.format

import com.everlog.android.common.kotlin.UnitUtils
import com.everlog.android.common.kotlin.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode

// A value with nothing entered
const val EmptyValue = "–"

/**
 * 62.5, 60, 137.79: at most two decimals, without trailing zeros.
 */
fun formatWeight(value: Float): String =
    BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * 0:30, 1:05, 1:02:03
 */
fun formatClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

fun unitLabel(unit: WeightUnit): String =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "--").orEmpty().uppercase()

// kg or lb after a number, or -- when the unit has no label
fun weightUnitAbbreviation(unit: WeightUnit): String? =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "--")

// kg or lb, as in sentences
fun unitName(unit: WeightUnit): String =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "").orEmpty()

fun displayWeight(kg: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.kgToLb(kg) else kg

fun storedWeight(value: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.lbToKg(value) else value
