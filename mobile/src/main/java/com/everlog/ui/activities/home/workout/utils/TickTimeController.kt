package com.everlog.ui.activities.home.workout.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.utils.Utils
import com.everlog.utils.device.DeviceUtils
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask


class TickTimeController(context: Context, callback: OnTimeTick) {

    private val TAG = "TickTimeController"

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action || Intent.ACTION_SCREEN_ON == intent.action) {
                toggleDisplayBasedTimer()
            }
        }
    }

    private val mContext = WeakReference(context)
    private val mCallback: OnTimeTick? = callback
    private var mTicker: Timer? = null

    init {
        setupBroadcastReceivers()
    }

    fun ensureTimerWhenActivityResumed() {
        startTimer()
    }

    fun cancelTimer() {
        LocalBroadcastManager.getInstance(mContext.get()!!).unregisterReceiver(mBroadcastReceiver)
        stopTimer()
        Timber.tag(TAG).i("Cancelled workout timer")
    }

    private fun startTimer() {
        stopTimer()
        val handler = Handler()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    mCallback?.onTimeTick()
                }
            }
        }
        mTicker = Timer(false)
        mTicker?.scheduleAtFixedRate(timerTask, 0, 1000)
    }

    private fun stopTimer() {
        if (mTicker != null) {
            try {
                mTicker?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mTicker = null
            }
        }
    }

    private fun toggleDisplayBasedTimer() {
        if (Utils.isValidContext(mContext.get())) {
            if (DeviceUtils.isScreenOn(mContext.get())) {
                Timber.tag(TAG).i("Screen state changed to ON. Starting timer")
                startTimer()
            } else {
                Timber.tag(TAG).i("Screen state changed to OFF. Stopping timer")
                stopTimer()
            }
        }
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        LocalBroadcastManager.getInstance(mContext.get()!!).registerReceiver(mBroadcastReceiver, filter)
    }

    interface OnTimeTick {

        fun onTimeTick()
    }
}
