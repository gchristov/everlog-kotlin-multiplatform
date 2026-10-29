package com.everlog.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class Utils {

    public static void runWithDelay(Runnable runnable, int delay) {
        if (delay <= 0) {
            runnable.run();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, delay);
        }
    }

    public static void runInForeground(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public static void runInBackground(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static boolean isValidContext(Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            return !((Activity) context).isDestroyed() && !((Activity) context).isFinishing();
        }
        return true;
    }
}
