package com.everlog.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.everlog.utils.device.DeviceUtils


class VibrationUtils {

    companion object {

        private const val VIBRATION_DURATION = 500L

        fun vibrate(context: Context) {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (DeviceUtils.isAndroidO()) {
                v?.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Deprecated in API 26.
                v?.vibrate(VIBRATION_DURATION)
            }
        }
    }
}