package com.everlog.managers;

import android.util.Log;

import com.everlog.BuildConfig;
import com.everlog.application.ELApplication;
import com.everlog.constants.ELConstants;
import com.everlog.logging.CrashlyticsTree;
import com.everlog.logging.HyperlogTree;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hypertrack.hyperlog.HyperLog;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ErrorManager {

    private static final String TAG = "ErrorManager";

    public static final ErrorManager manager = new ErrorManager();

    private ErrorManager() {
        // No-op
    }

    public void initialize() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DISABLE_CRASHLYTICS);
        if (BuildConfig.DISABLE_CRASHLYTICS) {
//            Timber.plant(new Timber.DebugTree());
        } else {
            // Add Crashlytics
            Timber.plant(new CrashlyticsTree());
            // Add Hyperlog
            HyperLog.initialize(ELApplication.getInstance(), (int) TimeUnit.HOURS.toSeconds(24));
            HyperLog.setLogLevel(Log.VERBOSE);
            Timber.plant(new HyperlogTree());
        }
        Timber.tag(TAG).i("Crashlytics disabled: %s", BuildConfig.DISABLE_CRASHLYTICS);
    }

    public void userIdentify(String userId, String email, String name) {
        // Crashlytics
        FirebaseCrashlytics.getInstance().setUserId(userId);
        FirebaseCrashlytics.getInstance().setCustomKey(ELConstants.FIELD_CRASHLYTICS_USER_ID, userId);
        FirebaseCrashlytics.getInstance().setCustomKey(ELConstants.FIELD_CRASHLYTICS_USER_EMAIL, email);
        FirebaseCrashlytics.getInstance().setCustomKey(ELConstants.FIELD_CRASHLYTICS_USER_NAME, name);
        // Bugfender
//        Bugfender.setDeviceString("user.id", userId);
//        Bugfender.setDeviceString("user.email", email);
//        Bugfender.setDeviceString("user.name", name);
    }
}
