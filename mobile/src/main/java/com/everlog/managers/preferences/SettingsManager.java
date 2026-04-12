package com.everlog.managers.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.everlog.utils.ArrayResourceTypeUtils;

import org.threeten.bp.DayOfWeek;

public class SettingsManager extends PreferencesManager {

    private enum PreferenceKeys {
        WEIGHTS_INCREASE,
        REPS_INCREASE,
        TIME_INCREASE,
        FIRST_DAY_OF_WEEK,
        WEEKLY_WORKOUTS_GOAL,
        LOGGED_IN,
        UNIT_WEIGHT,
        MUSCLE_GOAL,
    }

    public enum WeightUnit {
        KILOGRAM,
        POUND
    }

    public enum MuscleGoal {
        // https://www.menshealth.com/uk/building-muscle/a748257/how-to-calculate-one-rep-max/
        NONE,
        HISTORY,
        EXPLOSIVENESS, // 50% of 1RM
        ENDURANCE, // 70% of 1RM
        GROWTH, // 80% of 1RM
        POWER, // 90% of 1RM
        STRENGTH; // 95% of 1RM

        public static MuscleGoal[] availableGoals() {
            return new MuscleGoal[]{NONE,
                    HISTORY,
                    EXPLOSIVENESS,
                    GROWTH,
                    POWER};
        }

        public boolean proLocked() {
            switch (this) {
                case NONE:
                case HISTORY:
                    return false;
                default:
                    return true;
            }
        }

        public boolean muscleGoalSet() {
            return proLocked();
        }

        public boolean canPrefill() {
            return this != NONE;
        }

        public boolean is1RMBased() {
            return canPrefill() && this != HISTORY;
        }

        public float percent1RM() {
            switch (this) {
                case EXPLOSIVENESS:
                    return 0.5f;
                case ENDURANCE:
                    return 0.7f;
                case GROWTH:
                    return 0.8f;
                case POWER:
                    return 0.9f;
                case STRENGTH:
                    return 0.95f;
            }
            return 0f;
        }

        public String percent1RMSummary(Context context) {
            switch (this) {
                case NONE:
                case HISTORY:
                    return valueBenefit(context);
                default:
                    return String.format("%s  •  %.0f%% 1RM", valueBenefit(context), percent1RM() * 100);
            }
        }

        public String valueName(Context context) {
            switch (this) {
                case NONE:
                    return "None";
                case HISTORY:
                    return "Normal";
                case EXPLOSIVENESS:
                    return "Explosiveness";
                case ENDURANCE:
                    return "Endurance";
                case GROWTH:
                    return "Growth";
                case POWER:
                    return "Power";
                case STRENGTH:
                    return "Strength";
            }
            return null;
        }

        public String valueSettingsSummary(Context context, boolean includeMuscleWord) {
            switch (this) {
                case NONE:
                    return "No weight suggestions";
                case HISTORY:
                    return "Weight suggested from history";
                case EXPLOSIVENESS:
                    return "Weight optimised for " + (includeMuscleWord ? "muscle " : "") + "pace and speed";
                case ENDURANCE:
                    return "Weight optimised for weight loss and " + (includeMuscleWord ? "muscle " : "") + "endurance";
                case GROWTH:
                    return "Weight optimised for " + (includeMuscleWord ? "muscle " : "") + "growth";
                case POWER:
                    return "Weight optimised for maximum " + (includeMuscleWord ? "muscle " : "") + "force";
                case STRENGTH:
                    return "Weight optimised for maximum " + (includeMuscleWord ? "muscle " : "") + "strength";
            }
            return null;
        }

        private String valueBenefit(Context context) {
            switch (this) {
                case NONE:
                    return "No weight suggestions";
                case HISTORY:
                    return "Weight suggested from history";
                case EXPLOSIVENESS:
                    return "Improves muscle explosive power";
                case ENDURANCE:
                    return "Great for weight loss and muscle endurance";
                case GROWTH:
                    return "Stimulates muscle growth";
                case POWER:
                    return "Creates maximum force";
                case STRENGTH:
                    return "Helps you push past your old limits";
            }
            return null;
        }
    }

    public static final SettingsManager manager = new SettingsManager();

    public static String weightUnitAbbreviation() {
        return ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(manager.weightUnit().name(), "--");
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
        editor.apply();
    }

    public float weightIncrease() {
        return getPreference(PreferenceKeys.WEIGHTS_INCREASE.name(), 1f);
    }

    public void setWeightIncrease(float value) {
        savePreference(value, PreferenceKeys.WEIGHTS_INCREASE.name());
    }

    public int repsIncrease() {
        return getPreference(PreferenceKeys.REPS_INCREASE.name(), 1);
    }

    public void setRepsIncrease(int value) {
        savePreference(value, PreferenceKeys.REPS_INCREASE.name());
    }

    public int timeIncrease() {
        return getPreference(PreferenceKeys.TIME_INCREASE.name(), 1);
    }

    public void setTimeIncrease(int value) {
        savePreference(value, PreferenceKeys.TIME_INCREASE.name());
    }

    public DayOfWeek firstDayOfWeek() {
        return DayOfWeek.of(getPreference(PreferenceKeys.FIRST_DAY_OF_WEEK.name(), DayOfWeek.MONDAY.getValue()));
    }

    public void setFirstDayOfWeek(DayOfWeek value) {
        savePreference(value.getValue(), PreferenceKeys.FIRST_DAY_OF_WEEK.name());
    }

    public int weeklyWorkoutsGoal() {
        return getPreference(PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name(), 3);
    }

    public void setWeeklyWorkoutsGoal(int value) {
        savePreference(value, PreferenceKeys.WEEKLY_WORKOUTS_GOAL.name());
    }

    public boolean loggedIn() {
        return getPreference(PreferenceKeys.LOGGED_IN.name(), false);
    }

    public void setLoggedIn(boolean value) {
        savePreference(value, PreferenceKeys.LOGGED_IN.name());
    }

    public WeightUnit weightUnit() {
        return WeightUnit.valueOf(getPreference(PreferenceKeys.UNIT_WEIGHT.name(), WeightUnit.KILOGRAM.name()));
    }

    public void setWeightUnit(WeightUnit value) {
        savePreference(value.name(), PreferenceKeys.UNIT_WEIGHT.name());
    }

    public MuscleGoal muscleGoal() {
        return MuscleGoal.valueOf(getPreference(PreferenceKeys.MUSCLE_GOAL.name(), MuscleGoal.HISTORY.name()));
    }

    public void setMuscleGoal(MuscleGoal value) {
        savePreference(value.name(), PreferenceKeys.MUSCLE_GOAL.name());
    }
}
