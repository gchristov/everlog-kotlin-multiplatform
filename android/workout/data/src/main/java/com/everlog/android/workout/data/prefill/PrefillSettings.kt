package com.everlog.android.workout.data.prefill

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.settings.data.UserSettings

// The settings a prefill fills sets in with, read when it runs
data class PrefillSettings(val weightUnit: WeightUnit, val muscleGoal: MuscleGoal)

fun UserSettings.prefillSettings() = PrefillSettings(weightUnit(), muscleGoal())
