package com.everlog.android.common.kotlin.format

import android.content.Context
import com.everlog.android.common.kotlin.R
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.append
import java.util.concurrent.TimeUnit

private const val SEPARATOR = " • "

// 5 exercises • 15 sets
fun routineSummary(context: Context, exercises: Int, sets: Int): String = ""
    .append(SEPARATOR, context.resources.getQuantityString(R.plurals.exercises, exercises, exercises))
    .append(SEPARATOR, context.resources.getQuantityString(R.plurals.sets, sets, sets))

/**
 * 1000 lb • 34 reps • 1:30 m, leaving out what's zero.
 *
 * @param weight The total weight, already in [unit]
 */
fun exerciseSummary(context: Context, weight: Float, reps: Int, timeSeconds: Int, unit: WeightUnit): String {
    var summary = ""
    if (weight != 0F) {
        summary = summary.append(SEPARATOR, "${StatsFormatUtils.formatWeightStatsLabel(weight)} ${weightUnitAbbreviation(unit)}")
    }
    if (reps != 0) {
        summary = summary.append(SEPARATOR, context.resources.getQuantityString(R.plurals.reps, reps, reps))
    }
    if (timeSeconds != 0) {
        summary = summary.append(SEPARATOR, "${StatsFormatUtils.formatTimeStatsLabel(TimeUnit.SECONDS.toMillis(timeSeconds.toLong()), "m:ss")} m")
    }
    return summary
}

/**
 * 0:45 h • 1000 lb
 *
 * @param weight The total weight, already in [unit], or null to leave it out
 */
fun workoutSummary(context: Context, durationMillis: Long, weight: Float?, unit: WeightUnit): String {
    var summary = "".append(SEPARATOR, "${StatsFormatUtils.formatTimeStatsLabel(durationMillis)} ${context.getString(R.string.hour)}")
    if (weight != null) {
        summary = summary.append(SEPARATOR, "${StatsFormatUtils.formatWeightStatsLabel(weight)} ${weightUnitAbbreviation(unit)}")
    }
    return summary
}
