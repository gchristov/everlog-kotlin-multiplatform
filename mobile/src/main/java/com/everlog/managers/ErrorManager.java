package com.everlog.managers;

import android.util.Log;

import com.everlog.application.ELApplication;
import com.everlog.constants.ELConstants;
import com.everlog.logging.CrashlyticsTree;
import com.everlog.logging.HyperlogTree;
import com.everlog.utils.device.DeviceUtils;
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
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        if (DeviceUtils.isFirebaseTestLabRun()) {
            FirebaseCrashlytics.getInstance().setCustomKey("is_running_under_test", true);
        }
        // Add Crashlytics
        Timber.plant(new CrashlyticsTree());
        // Add Hyperlog
        HyperLog.initialize(ELApplication.getInstance(), (int) TimeUnit.HOURS.toSeconds(24));
        HyperLog.setLogLevel(Log.VERBOSE);
        Timber.plant(new HyperlogTree());
    }

    public void userIdentify(String userId) {
        // Crashlytics
        FirebaseCrashlytics.getInstance().setUserId(userId);
        FirebaseCrashlytics.getInstance().setCustomKey(ELConstants.FIELD_CRASHLYTICS_USER_ID, userId);
    }
}
