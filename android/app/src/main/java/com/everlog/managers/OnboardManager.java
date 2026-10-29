package com.everlog.managers;

import com.everlog.managers.preferences.PreferencesManager;

public class OnboardManager extends PreferencesManager {

    public enum PreferenceKeys {
        SEEN_ONBOARD_EXERCISES_FILTER,
        SEEN_ONBOARD_GROUPS_OPTIONS_EXERCISE,
        SEEN_ONBOARD_GROUPS_OPTIONS_SET,
        SEEN_ONBOARD_WORKOUT_GOALS,
        SEEN_ONBOARD_WORKOUT_SET_TIMER,
    }

    public static final OnboardManager manager = new OnboardManager();

    public void setSeenOnboard(PreferenceKeys key, boolean value) {
        savePreference(value, key.name());
    }

    public boolean seenOnboard(PreferenceKeys key) {
        return getPreference(key.name(), false);
    }
}
