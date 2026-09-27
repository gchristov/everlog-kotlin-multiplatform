package com.everlog.managers.apprate;

import android.content.SharedPreferences;

import com.everlog.config.HomeNotification;
import com.everlog.managers.preferences.PreferencesManager;

import java.util.Date;

public class AppLaunchState extends PreferencesManager {

    private enum PreferenceKeys {
        LAST_LAUNCH_DATE,

        // Home notification

        HOME_NOTIFICATION_LAST_DISMISSED_ID,

        // Rate

        RATE_PROMPT_ACTIONS_SINCE_LAST,
        RATE_PROMPT_LAST_ACTION_ID,
        RATE_PROMPT_COUNT,
        RATE_PROMPT_LAST_DATE,

        // App update

        APP_UPDATE_DECLINED_VERSION_CODE,
        APP_UPDATE_DECLINED_DATE
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

    long getLastLaunchDate() {
        return getPreference(PreferenceKeys.LAST_LAUNCH_DATE.name(), -1L);
    }

    void setLastLaunchDate(Date date) {
        savePreference(date.getTime(), PreferenceKeys.LAST_LAUNCH_DATE.name());
    }

    // Home notification

    String homeNotificationLastDismissedId() {
        return getPreference(PreferenceKeys.HOME_NOTIFICATION_LAST_DISMISSED_ID.name(), (String) null);
    }

    void setHomeNotificationLastDismissedId(HomeNotification notification) {
        savePreference(notification.dismissalId(), PreferenceKeys.HOME_NOTIFICATION_LAST_DISMISSED_ID.name());
    }

    // Rate

    int ratePromptActionsSinceLast() {
        return getPreference(PreferenceKeys.RATE_PROMPT_ACTIONS_SINCE_LAST.name(), 0);
    }

    String ratePromptLastActionId() {
        return getPreference(PreferenceKeys.RATE_PROMPT_LAST_ACTION_ID.name(), (String) null);
    }

    void setRatePromptActionRecorded(int actionsSinceLast, String actionId) {
        savePreference(actionsSinceLast, PreferenceKeys.RATE_PROMPT_ACTIONS_SINCE_LAST.name());
        savePreference(actionId, PreferenceKeys.RATE_PROMPT_LAST_ACTION_ID.name());
    }

    int ratePromptCount() {
        return getPreference(PreferenceKeys.RATE_PROMPT_COUNT.name(), 0);
    }

    long ratePromptLastDate() {
        return getPreference(PreferenceKeys.RATE_PROMPT_LAST_DATE.name(), -1L);
    }

    void setRatePromptLaunched(int count, long date) {
        savePreference(count, PreferenceKeys.RATE_PROMPT_COUNT.name());
        savePreference(date, PreferenceKeys.RATE_PROMPT_LAST_DATE.name());
        savePreference(0, PreferenceKeys.RATE_PROMPT_ACTIONS_SINCE_LAST.name());
    }

    // App update

    int appUpdateDeclinedVersionCode() {
        return getPreference(PreferenceKeys.APP_UPDATE_DECLINED_VERSION_CODE.name(), -1);
    }

    long appUpdateDeclinedDate() {
        return getPreference(PreferenceKeys.APP_UPDATE_DECLINED_DATE.name(), -1L);
    }

    void setAppUpdateDeclined(int versionCode, Date date) {
        savePreference(versionCode, PreferenceKeys.APP_UPDATE_DECLINED_VERSION_CODE.name());
        savePreference(date.getTime(), PreferenceKeys.APP_UPDATE_DECLINED_DATE.name());
    }
}
