package com.everlog.android.common.data.model.set

import com.everlog.android.common.kotlin.format.SetValues

/**
 * What the set shows: its reps or its time, with its weight when one was entered. With [useTemplate], a value
 * not entered yet shows the template's target instead.
 */
fun ELSet.summaryValues(useTemplate: Boolean): SetValues {
    val weightKg = getWeightKg()?.takeIf { isWeightEntered() }
    return when {
        isRepBased() -> SetValues(reps = if (isRepsEntered() || !useTemplate) getReps() else getRequiredReps(), weightKg = weightKg)
        isTimeBased() -> SetValues(timeSeconds = if (isTimeEntered() || !useTemplate) getTimeSeconds() else getRequiredTimeSeconds(), weightKg = weightKg)
        else -> SetValues()
    }
}
