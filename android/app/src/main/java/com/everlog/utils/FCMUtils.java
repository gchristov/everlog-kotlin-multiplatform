package com.everlog.utils;

import com.everlog.data.datastores.ELDatastore;
import com.everlog.data.model.ELDevice;
import com.everlog.managers.DeviceManager;
import com.everlog.managers.auth.LocalUserManager;
import com.everlog.utils.device.DeviceUtils;
import com.google.firebase.firestore.SetOptions;
//import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Date;

import timber.log.Timber;

public class FCMUtils {

    private static final String TAG = "FCMUtils";

    public static void refreshFCMToken() {
//        FirebaseInstanceId.getInstance().getInstanceId()
//                .addOnCompleteListener(task -> {
//                    if (!task.isSuccessful() || task.getResult() == null) {
//                        Timber.tag(TAG).w(task.getException(), "getInstanceId failed");
//                        return;
//                    }
//                    // Get new Instance ID token
//                    String token = task.getResult().getToken();
//                    refreshTokenForLoggedInUser(token);
//                });
    }

    public static void refreshTokenForLoggedInUser(String token) {
        Timber.tag(TAG).i("Updating user device info");
        Timber.tag(TAG).d("FCM token: %s", token);
        if (LocalUserManager.hasUser()) {
            ELDevice device = ELDevice.newDevice(DeviceManager.manager.deviceId(), DeviceUtils.getDeviceName(), token, new Date().getTime());
            ELDatastore.deviceStore().create(device, SetOptions.merge());
            Timber.tag(TAG).i("Updated user device info");
        } else {
            Timber.tag(TAG).w("User not logged in. Ignoring device token");
        }
    }
}
