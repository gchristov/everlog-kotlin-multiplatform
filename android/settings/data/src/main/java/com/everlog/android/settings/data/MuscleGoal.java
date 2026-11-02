package com.everlog.android.settings.data;

import android.content.Context;

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
