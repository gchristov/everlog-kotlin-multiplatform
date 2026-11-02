package com.everlog.android.settings.data;

import android.content.SharedPreferences;

import com.everlog.android.common.kotlin.WeightUnit;
import com.everlog.android.common.persistence.PreferencesManager;

import org.threeten.bp.DayOfWeek;

public class RealUserSettings extends PreferencesManager implements UserSettings {

    private enum PreferenceKeys {
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

    public void clearUserPreferences() {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(PreferenceKeys.WEIGHTS_INCREASE.name());
        editor.remove(PreferenceKeys.REPS_INCREASE.name());
        editor.remove(PreferenceKeys.TIME_INCREASE.name());
        editor.remove(PreferenceKeys.FIRST_DAY_OF_WEEK.name());
        editor.remove(PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name());
        editor.remove(PreferenceKeys.UNIT_WEIGHT.name());
        editor.remove(PreferenceKeys.MUSCLE_GOAL.name());
        editor.remove(PreferenceKeys.KEEP_SCREEN_ON.name());
        editor.remove(PreferenceKeys.ONBOARDING_PENDING.name());
        editor.apply();
    }

    @Override
    public float weightIncrease() {
        return getPreference(PreferenceKeys.WEIGHTS_INCREASE.name(), 1f);
    }

    @Override
    public void setWeightIncrease(float value) {
        savePreference(value, PreferenceKeys.WEIGHTS_INCREASE.name());
    }

    @Override
    public int repsIncrease() {
        return getPreference(PreferenceKeys.REPS_INCREASE.name(), 1);
    }

    @Override
    public void setRepsIncrease(int value) {
        savePreference(value, PreferenceKeys.REPS_INCREASE.name());
    }

    @Override
    public int timeIncrease() {
        return getPreference(PreferenceKeys.TIME_INCREASE.name(), 1);
    }

    @Override
    public void setTimeIncrease(int value) {
        savePreference(value, PreferenceKeys.TIME_INCREASE.name());
    }

    @Override
    public DayOfWeek firstDayOfWeek() {
        return DayOfWeek.of(getPreference(PreferenceKeys.FIRST_DAY_OF_WEEK.name(), DayOfWeek.MONDAY.getValue()));
    }

    @Override
    public void setFirstDayOfWeek(DayOfWeek value) {
        savePreference(value.getValue(), PreferenceKeys.FIRST_DAY_OF_WEEK.name());
    }

    @Override
    public int weeklyWorkoutsGoal() {
        return getPreference(PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name(), 3);
    }

    @Override
    public void setWeeklyWorkoutsGoal(int value) {
        savePreference(value, PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name());
    }

    public boolean loggedIn() {
        return getPreference(PreferenceKeys.LOGGED_IN.name(), false);
    }

    public void setLoggedIn(boolean value) {
        savePreference(value, PreferenceKeys.LOGGED_IN.name());
    }

    // A new account that hasn't been through onboarding yet, e.g. because the app was closed during
    // it. Set and cleared by whichever screen opens onboarding.
    public boolean onboardingPending() {
        return getPreference(PreferenceKeys.ONBOARDING_PENDING.name(), false);
    }

    public void setOnboardingPending(boolean value) {
        savePreference(value, PreferenceKeys.ONBOARDING_PENDING.name());
    }

    @Override
    public WeightUnit weightUnit() {
        return WeightUnit.valueOf(getPreference(PreferenceKeys.UNIT_WEIGHT.name(), WeightUnit.KILOGRAM.name()));
    }

    @Override
    public void setWeightUnit(WeightUnit value) {
        savePreference(value.name(), PreferenceKeys.UNIT_WEIGHT.name());
    }

    @Override
    public MuscleGoal muscleGoal() {
        return MuscleGoal.valueOf(getPreference(PreferenceKeys.MUSCLE_GOAL.name(), MuscleGoal.HISTORY.name()));
    }

    @Override
    public void setMuscleGoal(MuscleGoal value) {
        savePreference(value.name(), PreferenceKeys.MUSCLE_GOAL.name());
    }

    @Override
    public boolean keepScreenOn() {
        return getPreference(PreferenceKeys.KEEP_SCREEN_ON.name(), true);
    }

    @Override
    public void setKeepScreenOn(boolean value) {
        savePreference(value, PreferenceKeys.KEEP_SCREEN_ON.name());
    }
}
