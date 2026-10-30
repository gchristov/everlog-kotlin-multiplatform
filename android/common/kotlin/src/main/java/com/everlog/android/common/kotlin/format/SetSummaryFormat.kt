package com.everlog.android.common.kotlin.format

import android.content.Context
import com.everlog.android.common.kotlin.R
import com.everlog.android.common.kotlin.WeightUnit

// What a set shows: its reps or its time, and its weight in kg when it has one
data class SetValues(
    val reps: Int? = null,
    val timeSeconds: Int? = null,
    val weightKg: Float? = null,
)

/**
 * 12 x 60 kg, 12 reps, 0:45 • 10 kg or 0:45. Null when the set has no reps or time to show.
 */
fun setSummary(context: Context?, values: SetValues, unit: WeightUnit): String? {
    val weight = values.weightKg?.let { "${FormatUtils.formatDecimal(displayWeight(it, unit))} ${weightUnitAbbreviation(unit)}" }
    values.reps?.let { reps ->
        if (reps <= 0) return null
        return if (weight != null) "$reps x $weight" else context?.resources?.getQuantityString(R.plurals.reps, reps, reps)
    }
    values.timeSeconds?.let { seconds ->
        if (seconds <= 0) return null
        val time = FormatUtils.formatSetTime(seconds)
        return if (weight != null) "$time • $weight" else time
    }
    return null
}
