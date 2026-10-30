package com.everlog.ui.components.exercisetable

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.utils.format.displayWeight
import com.everlog.utils.format.formatWeight

// Typing into a set's cells from the keypad. A typed value is kept as text until the keypad moves on, so these work on
// the text; the first key replaces the cell's value.

/**
 * The text after [key], or null when the key doesn't apply: a point in reps or time, or a second one in a weight.
 */
internal fun typedAfterKey(typed: String?, field: ValueField, key: Char): String? {
    val base = typed.orEmpty()
    if (key == '.' && (field != ValueField.Weight || '.' in base)) return null
    return (base + key).take(maxLength(field))
}

// Backspacing into a value that hasn't been typed starts from what it shows
internal fun typedAfterBackspace(typed: String?, set: WorkoutSet, field: ValueField, unit: WeightUnit): String =
    (typed ?: rawText(set, field, unit)).dropLast(1)

/**
 * The keypad's − and +: a rep, [weightStep] in the user's unit, or the same seconds as a running timer's. Never below 0.
 */
internal fun steppedValue(set: WorkoutSet, field: ValueField, unit: WeightUnit, weightStep: Float, direction: Int): Float {
    val (value, step) = when (field) {
        ValueField.Weight -> (set.weightKg?.let { displayWeight(it, unit) } ?: 0f) to weightStep
        ValueField.Reps -> (set.reps ?: 0).toFloat() to RepsStep
        ValueField.Time -> (set.timeSeconds ?: 0).toFloat() to TimeStepSeconds
    }
    return (value + direction * step).coerceAtLeast(0f)
}

private const val RepsStep = 1f
private val TimeStepSeconds = AppConfig.configuration.defaultRestTimeOffsetSeconds.toFloat()

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
