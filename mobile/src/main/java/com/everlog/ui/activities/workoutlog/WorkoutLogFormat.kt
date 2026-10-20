package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.UnitUtils
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 62.5, 60, 137.79: at most two decimals, without trailing zeros.
 */
internal fun formatWeight(value: Float): String =
    BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * 0:30, 1:05, 1:02:03
 */
internal fun formatClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/**
 * A time typed like a microwave's: the last two digits are seconds, so 130 is 1:30.
 */
internal fun typedSeconds(text: String): Int {
    val number = text.toIntOrNull() ?: return 0
    return if (text.length <= 2) number else (number / 100) * 60 + number % 100
}

/**
 * A time as the digits that type it, for backspacing into it: 90 seconds is 130.
 */
internal fun secondsTyped(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes > 0) "$minutes%02d".format(seconds % 60) else "$seconds"
}

internal fun unitLabel(unit: WeightUnit): String =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "--").orEmpty().uppercase()

internal fun displayWeight(kg: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.kgToLb(kg) else kg

internal fun storedWeight(value: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.lbToKg(value) else value

internal fun maxLength(field: ValueField) = when (field) {
    ValueField.Weight -> 6
    ValueField.Reps -> 3
    ValueField.Time -> 4
}

// Nothing typed clears the value
internal fun parse(field: ValueField, text: String): Float? = when (field) {
    ValueField.Weight -> text.toFloatOrNull()
    ValueField.Reps -> text.toIntOrNull()?.toFloat()
    ValueField.Time -> text.takeIf { it.isNotEmpty() }?.let { typedSeconds(it).toFloat() }
}

// For backspacing into a value that hasn't been typed
internal fun rawText(set: WorkoutSet, field: ValueField, unit: WeightUnit): String = when (field) {
    ValueField.Weight -> set.weightKg?.let { formatWeight(displayWeight(it, unit)) }.orEmpty()
    ValueField.Reps -> set.reps?.toString().orEmpty()
    ValueField.Time -> set.timeSeconds?.let(::secondsTyped).orEmpty()
}

/**
 * 15 × 17 kg, 0:30 × 10 kg, or 12 and 0:45 without a weight. Null when nothing was logged.
 */
internal fun lastTimeText(set: WorkoutSet, unit: WeightUnit, unitLabel: String): String? {
    val count = if (set.mode == SetMode.Time) set.timeSeconds?.let { formatClock(it.toLong()) } else set.reps?.toString()
    val weight = set.weightKg?.let { "${formatWeight(displayWeight(it, unit))} ${unitLabel.lowercase()}" }
    return listOfNotNull(count, weight).joinToString(" × ").ifEmpty { null }
}
