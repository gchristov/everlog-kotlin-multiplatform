package com.everlog.android.common.mvp.onboarding;

import com.everlog.android.common.persistence.PreferencesManager;

// Whether each screen tip was seen. A tip's key is the name it's saved under, so it never changes.
public class OnboardManager extends PreferencesManager {

    public static final OnboardManager manager = new OnboardManager();

    public void setSeenOnboard(String key, boolean value) {
        savePreference(value, key);
    }

    public boolean seenOnboard(String key) {
        return getPreference(key, false);
    }
}
