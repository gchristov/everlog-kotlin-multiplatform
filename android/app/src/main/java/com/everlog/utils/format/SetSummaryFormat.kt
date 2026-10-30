package com.everlog.utils.format

import android.content.Context
import com.everlog.R
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.kotlin.format.FormatUtils
import com.everlog.android.settings.data.SettingsManager

fun ELSet.getWorkoutDetailsSummary(context: Context?): String? {
    if (isWithoutData()) {
        return "Skipped"
    }
    return getExerciseSetSummary(context, false)
}

fun ELSet.getExerciseSetSummary(context: Context?, useTemplate: Boolean): String? {
    return when {
        isRepBased() -> {
            val finalReps: Int = if (isRepsEntered() || !useTemplate) getReps() else getRequiredReps()
            val hasReps = finalReps > 0
            when {
                hasReps -> {
                    if (isWeightEntered()) {
                        "$finalReps x ${formatWeight()}"
                    } else {
                        formatReps(context, finalReps)
                    }
                }
                else -> null
            }
        }
        isTimeBased() -> {
            val finalTimeSeconds: Int = if (isTimeEntered() || !useTemplate) getTimeSeconds() else getRequiredTimeSeconds()
            val hasTime = finalTimeSeconds > 0
            when {
                hasTime -> {
                    if (isWeightEntered()) {
                        "${formatTime(finalTimeSeconds)} • ${formatWeight()}"
                    } else {
                        formatTime(finalTimeSeconds)
                    }
                }
                else -> null
            }
        }
        else -> null
    }
}

private fun formatReps(context: Context?, reps: Int): String? {
    return context?.resources?.getQuantityString(R.plurals.reps, reps, reps)
}

private fun ELSet.formatWeight(): String? {
    return "${FormatUtils.formatDecimal(getWeight(SettingsManager.manager.weightUnit()))} ${weightUnitAbbreviation(SettingsManager.manager.weightUnit())}"
}

private fun formatTime(timeSeconds: Int): String? {
    return FormatUtils.formatSetTime(timeSeconds)
}
