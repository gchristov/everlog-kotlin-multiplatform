package com.everlog.android.settings.testfixtures

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.settings.data.UserSettings
import org.threeten.bp.DayOfWeek

// Starts with the same defaults as RealUserSettings
class FakeUserSettings(
    private var unit: WeightUnit = WeightUnit.KILOGRAM,
    private var weekStart: DayOfWeek = DayOfWeek.MONDAY,
    private var goal: MuscleGoal = MuscleGoal.HISTORY,
    private var weightStep: Float = 1f,
    private var repsStep: Int = 1,
    private var timeStep: Int = 1,
    private var workoutsGoal: Int = 3,
    private var screenOn: Boolean = true,
) : UserSettings {
    override fun weightUnit() = unit
    override fun setWeightUnit(value: WeightUnit) { unit = value }
    override fun firstDayOfWeek() = weekStart
    override fun setFirstDayOfWeek(value: DayOfWeek) { weekStart = value }
    override fun muscleGoal() = goal
    override fun setMuscleGoal(value: MuscleGoal) { goal = value }
    override fun weightIncrease() = weightStep
    override fun setWeightIncrease(value: Float) { weightStep = value }
    override fun repsIncrease() = repsStep
    override fun setRepsIncrease(value: Int) { repsStep = value }
    override fun timeIncrease() = timeStep
    override fun setTimeIncrease(value: Int) { timeStep = value }
    override fun weeklyWorkoutsGoal() = workoutsGoal
    override fun setWeeklyWorkoutsGoal(value: Int) { workoutsGoal = value }
    override fun keepScreenOn() = screenOn
    override fun setKeepScreenOn(value: Boolean) { screenOn = value }
}
