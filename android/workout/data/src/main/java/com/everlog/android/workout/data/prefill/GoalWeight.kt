package com.everlog.android.workout.data.prefill

import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The weight a 1RM goal suggests: its share of [oneRepMax], rounded up to a whole kg or lb, in the unit of
 * [oneRepMax].
 */
fun goalWeight(oneRepMax: Float, goal: MuscleGoal): Float = ceil(goal.percent1RM() * rounded1RM(oneRepMax) - 0.001f)

/**
 * The estimate to one decimal. It carries float noise (80 x 5 gives 90.01), which rounding a goal weight up would
 * turn into an extra kg.
 */
fun rounded1RM(oneRepMax: Float): Float = (oneRepMax * 10).roundToInt() / 10f
