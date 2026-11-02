package com.everlog.android.settings.data

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.persistence.PreferencesManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import org.threeten.bp.DayOfWeek

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserSettings>())
@Inject
class RealUserSettings : PreferencesManager(), UserSettings {

    private enum class PreferenceKeys {
        WEIGHTS_INCREASE,
        REPS_INCREASE,
        TIME_INCREASE,
        FIRST_DAY_OF_WEEK,
        WEEKLY_WORKOUTS_GOAL,
        LOGGED_IN,
        UNIT_WEIGHT,
        MUSCLE_GOAL,
        KEEP_SCREEN_ON,
        ONBOARDING_PENDING,
    }

    fun clearUserPreferences() {
        preferences.edit()
            .remove(PreferenceKeys.WEIGHTS_INCREASE.name)
            .remove(PreferenceKeys.REPS_INCREASE.name)
            .remove(PreferenceKeys.TIME_INCREASE.name)
            .remove(PreferenceKeys.FIRST_DAY_OF_WEEK.name)
            .remove(PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name)
            .remove(PreferenceKeys.UNIT_WEIGHT.name)
            .remove(PreferenceKeys.MUSCLE_GOAL.name)
            .remove(PreferenceKeys.KEEP_SCREEN_ON.name)
            .remove(PreferenceKeys.ONBOARDING_PENDING.name)
            .apply()
    }

    override fun weightIncrease(): Float = getPreference(PreferenceKeys.WEIGHTS_INCREASE.name, 1f)

    override fun setWeightIncrease(value: Float) = savePreference(value, PreferenceKeys.WEIGHTS_INCREASE.name)

    override fun repsIncrease(): Int = getPreference(PreferenceKeys.REPS_INCREASE.name, 1)

    override fun setRepsIncrease(value: Int) = savePreference(value, PreferenceKeys.REPS_INCREASE.name)

    override fun timeIncrease(): Int = getPreference(PreferenceKeys.TIME_INCREASE.name, 1)

    override fun setTimeIncrease(value: Int) = savePreference(value, PreferenceKeys.TIME_INCREASE.name)

    override fun firstDayOfWeek(): DayOfWeek =
        DayOfWeek.of(getPreference(PreferenceKeys.FIRST_DAY_OF_WEEK.name, DayOfWeek.MONDAY.value))

    override fun setFirstDayOfWeek(value: DayOfWeek) = savePreference(value.value, PreferenceKeys.FIRST_DAY_OF_WEEK.name)

    override fun weeklyWorkoutsGoal(): Int = getPreference(PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name, 3)

    override fun setWeeklyWorkoutsGoal(value: Int) = savePreference(value, PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name)

    fun loggedIn(): Boolean = getPreference(PreferenceKeys.LOGGED_IN.name, false)

    fun setLoggedIn(value: Boolean) = savePreference(value, PreferenceKeys.LOGGED_IN.name)

    // A new account that hasn't been through onboarding yet, e.g. because the app was closed during
    // it. Set and cleared by whichever screen opens onboarding.
    fun onboardingPending(): Boolean = getPreference(PreferenceKeys.ONBOARDING_PENDING.name, false)

    fun setOnboardingPending(value: Boolean) = savePreference(value, PreferenceKeys.ONBOARDING_PENDING.name)

    override fun weightUnit(): WeightUnit =
        WeightUnit.valueOf(getPreference(PreferenceKeys.UNIT_WEIGHT.name, WeightUnit.KILOGRAM.name))

    override fun setWeightUnit(value: WeightUnit) = savePreference(value.name, PreferenceKeys.UNIT_WEIGHT.name)

    override fun muscleGoal(): MuscleGoal =
        MuscleGoal.valueOf(getPreference(PreferenceKeys.MUSCLE_GOAL.name, MuscleGoal.HISTORY.name))

    override fun setMuscleGoal(value: MuscleGoal) = savePreference(value.name, PreferenceKeys.MUSCLE_GOAL.name)

    override fun keepScreenOn(): Boolean = getPreference(PreferenceKeys.KEEP_SCREEN_ON.name, true)

    override fun setKeepScreenOn(value: Boolean) = savePreference(value, PreferenceKeys.KEEP_SCREEN_ON.name)
}
