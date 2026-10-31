package com.everlog.android.onboarding

import com.everlog.android.common.kotlin.WeightUnit

/**
 * The app settings some answers set, written as soon as the question is answered: the units, and
 * days a week as the weekly workouts goal. A change logs the same event as in Settings.
 */
interface OnboardingSettings {
    fun setWeightUnit(unit: WeightUnit)

    fun setWeeklyWorkoutsGoal(count: Int)
}
