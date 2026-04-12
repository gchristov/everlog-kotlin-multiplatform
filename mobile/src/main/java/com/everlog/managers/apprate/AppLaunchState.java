package com.everlog.managers.apprate;

import android.content.SharedPreferences;

import com.everlog.config.HomeNotification;
import com.everlog.managers.preferences.PreferencesManager;

import java.util.Date;

public class AppLaunchState extends PreferencesManager {

    private enum PreferenceKeys {
        FIRST_LAUNCH_DATE,
        LAST_LAUNCH_DATE,
        DAYS_SINCE_LAST_LAUNCH,
        LAUNCH_COUNT,
        CONSECUTIVE_DAYS_LAUNCH_COUNT,

        // Home notification

        HOME_NOTIFICATION_LAST_HASH,

        // Rate

        RATE_SCHEDULED_TO_SHOW,
        RATE_LAST_PROMPT_DATE
    }

    public static final AppLaunchState state = new AppLaunchState();

    void clearState() {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        for (PreferenceKeys value : PreferenceKeys.values()) {
            editor.remove(value.name());
        }
        editor.apply();
    }

    int getConsecutiveDaysLaunchCount() {
        return getPreference(PreferenceKeys.CONSECUTIVE_DAYS_LAUNCH_COUNT.name(), 1);
    }

    void setConsecutiveDaysLaunchCount(int count) {
        savePreference(count, PreferenceKeys.CONSECUTIVE_DAYS_LAUNCH_COUNT.name());
    }

    int getLaunchCount() {
        return getPreference(PreferenceKeys.LAUNCH_COUNT.name(), 0);
    }

    void setLaunchCount(int count) {
        savePreference(count, PreferenceKeys.LAUNCH_COUNT.name());
    }

    long getDaysSinceLastLaunch() {
        return getPreference(PreferenceKeys.DAYS_SINCE_LAST_LAUNCH.name(), 0L);
    }

    void setDaysSinceLastLaunch(long days) {
        savePreference(days, PreferenceKeys.DAYS_SINCE_LAST_LAUNCH.name());
    }

    long getLastLaunchDate() {
        return getPreference(PreferenceKeys.LAST_LAUNCH_DATE.name(), -1L);
    }

    void setLastLaunchDate(Date date) {
        savePreference(date.getTime(), PreferenceKeys.LAST_LAUNCH_DATE.name());
    }

    long getFirstLaunchDate() {
        return getPreference(PreferenceKeys.FIRST_LAUNCH_DATE.name(), -1L);
    }

    void setFirstLaunchDate(Date date) {
        savePreference(date.getTime(), PreferenceKeys.FIRST_LAUNCH_DATE.name());
    }

    // Home notification

    int homeNotificationLastHash() {
        return getPreference(PreferenceKeys.HOME_NOTIFICATION_LAST_HASH.name(), -1);
    }

    void setHomeNotificationLastHash(HomeNotification notification) {
        savePreference(notification.hashCode(), PreferenceKeys.HOME_NOTIFICATION_LAST_HASH.name());
    }

    // Rate

    boolean rateScheduledToShow() {
        return getPreference(PreferenceKeys.RATE_SCHEDULED_TO_SHOW.name(), false);
    }

    void setRateScheduledToShow(boolean value) {
        savePreference(value, PreferenceKeys.RATE_SCHEDULED_TO_SHOW.name());
    }

    long rateLastPromptDate() {
        return getPreference(PreferenceKeys.RATE_LAST_PROMPT_DATE.name(), -1L);
    }

    void setRateLastPromptDate(Date date) {
        savePreference(date.getTime(), PreferenceKeys.RATE_LAST_PROMPT_DATE.name());
    }
}
