package com.everlog.managers;

import android.content.Intent;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.everlog.BuildConfig;
import com.everlog.R;
import com.everlog.application.ELApplication;
import com.everlog.config.AppUsageNotification;
import com.everlog.config.HomeNotification;
import com.everlog.config.RemoteConfig;
import com.everlog.constants.ELConstants;
import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.preferences.PreferencesManager;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;

import timber.log.Timber;

public class RemoteConfigManager extends PreferencesManager {

    private static final String TAG = "RemoteConfigManager";

    private enum PreferenceKeys {
        REMOTE_CONFIG,
    }

    public static final RemoteConfigManager manager = new RemoteConfigManager();

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private RemoteConfigManager() {
        setupFirebaseConfig();
        initLocalAppConfig();
    }

    // Shortcut methods

    public HomeNotification notificationHome() {
        RemoteConfig localConfig = loadLocalRemoteConfig();
        if (localConfig != null) {
            return localConfig.getNotificationHome();
        }
        return null;
    }

    public AppUsageNotification notificationAppUsage() {
        RemoteConfig localConfig = loadLocalRemoteConfig();
        if (localConfig != null) {
            AppUsageNotification notification = localConfig.getNotificationAppUsage();
            if (notification != null && notification.isValid()) {
                return notification;
            }
        }
        return null;
    }

    // Config refresh

    public void refreshAppConfig() {
        mFirebaseRemoteConfig.fetch()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // After config data is successfully fetched, it must be activated before newly fetched
                        // values are returned.
                        AnalyticsManager.manager.remoteConfigFetched();
                        mFirebaseRemoteConfig.activate().addOnCompleteListener(command -> {
                            refreshLocalAppConfig();
                        });
                    } else {
                        refreshLocalAppConfig();
                    }
                });
    }

    private void initLocalAppConfig() {
        checkLocalConfig();
    }

    private void refreshLocalAppConfig() {
        checkLocalConfig();

        RemoteConfig remoteConfig = buildRemoteConfig();
        RemoteConfig currentConfig = loadLocalRemoteConfig();
        if (!remoteConfig.equals(currentConfig)) {
            // Config has changed.
            currentConfig = remoteConfig;
            saveLocalRemoteConfig(currentConfig);
            Timber.tag(TAG).i("Remote config changed");
        }
        Timber.tag(TAG).d("Remote config refreshed");
        LocalBroadcastManager.getInstance(ELApplication.getInstance()).sendBroadcast(new Intent(ELConstants.BROADCAST_REMOTE_CONFIG_REFRESHED));
    }

    private void checkLocalConfig() {
        RemoteConfig currentConfig = loadLocalRemoteConfig();
        if (currentConfig == null) {
            // No previous app configuration saved, so save this one.
            currentConfig = buildRemoteConfig();
            saveLocalRemoteConfig(currentConfig);
        }
    }

    private RemoteConfig buildRemoteConfig() {
        return new RemoteConfig().populateFromSource(mFirebaseRemoteConfig);
    }

    // Preferences

    private RemoteConfig loadLocalRemoteConfig() {
        String json = getPreference(PreferenceKeys.REMOTE_CONFIG.name(), "");
        if (!TextUtils.isEmpty(json)) {
            Gson gson = new Gson();
            try {
                return gson.fromJson(json, RemoteConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
                Timber.tag(TAG).e(e);
            }
        }
        return null;
    }

    private void saveLocalRemoteConfig(RemoteConfig config) {
        Gson gson = new Gson();
        String json = gson.toJson(config);
        savePreference(json, PreferenceKeys.REMOTE_CONFIG.name());
    }

    // Setup

    private void setupFirebaseConfig() {
        long fetchIntervalSeconds = 3600; // 1 hour
        if (BuildConfig.DEBUG) {
            // If in developer mode, interval is set to 0, so each fetch will retrieve values from the service
            fetchIntervalSeconds = 0;
        }
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.firebase_remote_config_defaults);
    }
}
