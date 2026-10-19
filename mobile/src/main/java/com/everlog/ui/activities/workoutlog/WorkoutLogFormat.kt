package com.everlog.ui.activities.workoutlog

import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.utils.ArrayResourceTypeUtils
import java.math.BigDecimal
import java.math.RoundingMode

// How the workout screen writes and reads its numbers

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

/**
 * KG or LB, as the column and the keypad label it.
 */
internal fun unitLabel(unit: WeightUnit): String =
    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit.name, "--").orEmpty().uppercase()
