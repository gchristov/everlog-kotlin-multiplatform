package com.everlog.utils.format

import com.everlog.android.common.kotlin.UnitUtils
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.utils.ArrayResourceTypeUtils
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

// kg or lb, as in sentences
fun unitName(unit: WeightUnit): String =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "").orEmpty()

fun displayWeight(kg: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.kgToLb(kg) else kg

fun storedWeight(value: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.lbToKg(value) else value

/**
 * 15 × 17 kg, 0:30 × 10 kg, or 12 and 0:45 without a weight. Null when nothing was logged.
 */
fun lastTimeText(set: WorkoutSet, unit: WeightUnit, unitLabel: String): String? {
    val count = if (set.mode == SetMode.Time) set.timeSeconds?.let { formatClock(it.toLong()) } else set.reps?.toString()
    val weight = set.weightKg?.let { "${formatWeight(displayWeight(it, unit))} ${unitLabel.lowercase()}" }
    return listOfNotNull(count, weight).joinToString(" × ").ifEmpty { null }
}
