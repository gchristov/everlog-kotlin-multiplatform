package com.everlog.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.everlog.utils.SoundUtils.Companion.playTimerSound
import com.everlog.utils.VibrationUtils.Companion.vibrate
import timber.log.Timber

class WorkoutTimerReceiver : BroadcastReceiver() {

    private val TAG = "WorkoutTimerReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.tag(TAG).i("Received request to notify workout timer")
        if (context != null) {
            playTimerSound(context)
            vibrate(context)
        }
    }
}