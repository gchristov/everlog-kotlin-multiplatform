package com.everlog.utils.format

import android.content.Context
import com.everlog.R
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.append
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.settings.data.SettingsManager
import java.util.concurrent.TimeUnit

private const val SEPARATOR = " • "

fun ELRoutine.summary(context: Context): String {
    var summary = ""
    val exercisesCount = getTotalExercises()
    summary = summary.append(SEPARATOR, context.resources.getQuantityString(R.plurals.exercises, exercisesCount, exercisesCount))
    val setsCount = getTotalSets()
    summary = summary.append(SEPARATOR, context.resources.getQuantityString(R.plurals.sets, setsCount, setsCount))
    return summary
}

fun ELRoutineExercise.summary(context: Context): String {
    val reps = getTotalReps()
    val weight = getTotalWeight(SettingsManager.manager.weightUnit())
    val timeSeconds = getTotalTimeSeconds()
    var summary = ""
    if (weight != 0F) {
        val weightSummary = String.format("%s %s", StatsFormatUtils.formatWeightStatsLabel(weight), weightUnitAbbreviation(SettingsManager.manager.weightUnit()))
        summary = summary.append(SEPARATOR, weightSummary)
    }
    if (reps != 0) {
        summary = summary.append(SEPARATOR, context.resources.getQuantityString(R.plurals.reps, reps, reps))
    }
    if (timeSeconds != 0) {
        val timeSummary = String.format("%s %s", StatsFormatUtils.formatTimeStatsLabel(TimeUnit.SECONDS.toMillis(timeSeconds.toLong()), "m:ss"), "m")
        summary = summary.append(SEPARATOR, timeSummary)
    }
    return summary
}

fun ELWorkout.summary(context: Context): String {
    var summary = ""
    val durationSummary = String.format("%s %s", StatsFormatUtils.formatTimeStatsLabel(getDurationMillis()), context.getString(R.string.hour))
    summary = summary.append(SEPARATOR, durationSummary)
    if (hasWeight()) {
        val weightSummary = String.format("%s %s", StatsFormatUtils.formatWeightStatsLabel(getTotalWeight(SettingsManager.manager.weightUnit())), weightUnitAbbreviation(SettingsManager.manager.weightUnit()))
        summary = summary.append(SEPARATOR, weightSummary)
    }
    return summary
}
