package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.utils.format.displayWeight
import com.everlog.utils.format.formatWeight

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
