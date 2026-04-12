package com.everlog.ui.dialog;

import android.content.Context;
import android.widget.Toast;

public class ToastBuilder {

    public static void showToast(Context context, int messageResId) {
        showToast(context, context.getString(messageResId));
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, false);
    }

    public static void showToast(Context context,
                                 String message,
                                 boolean isLong) {
        Toast.makeText(context, message, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
