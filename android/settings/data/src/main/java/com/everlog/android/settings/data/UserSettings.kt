package com.everlog.android.settings.data

import com.everlog.android.common.kotlin.WeightUnit
import org.threeten.bp.DayOfWeek

interface UserSettings {
    fun weightUnit(): WeightUnit
    fun setWeightUnit(value: WeightUnit)
    fun firstDayOfWeek(): DayOfWeek
    fun setFirstDayOfWeek(value: DayOfWeek)
    fun muscleGoal(): MuscleGoal
    fun setMuscleGoal(value: MuscleGoal)
    fun weightIncrease(): Float
    fun setWeightIncrease(value: Float)
    fun repsIncrease(): Int
    fun setRepsIncrease(value: Int)
    fun timeIncrease(): Int
    fun setTimeIncrease(value: Int)
    fun weeklyWorkoutsGoal(): Int
    fun setWeeklyWorkoutsGoal(value: Int)
    fun keepScreenOn(): Boolean
    fun setKeepScreenOn(value: Boolean)
}
