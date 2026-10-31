package com.everlog.android.common.sessionui.exercisetable

import com.everlog.android.common.kotlin.format.unitLabel
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.WorkoutSet
import com.everlog.android.common.kotlin.format.displayWeight
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.kotlin.format.formatWeight

/**
 * 15 × 17 kg, 0:30 × 10 kg, or 12 and 0:45 without a weight. Null when nothing was logged.
 */
fun lastTimeText(set: WorkoutSet, unit: WeightUnit, unitLabel: String): String? {
    val count = if (set.mode == SetMode.Time) set.timeSeconds?.let { formatClock(it.toLong()) } else set.reps?.toString()
    val weight = set.weightKg?.let { "${formatWeight(displayWeight(it, unit))} ${unitLabel.lowercase()}" }
    return listOfNotNull(count, weight).joinToString(" × ").ifEmpty { null }
}
