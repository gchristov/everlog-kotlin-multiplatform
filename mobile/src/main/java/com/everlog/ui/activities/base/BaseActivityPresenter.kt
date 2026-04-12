package com.everlog.ui.activities.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELConstants
import com.everlog.ui.mvp.BaseBroadcastPresenter

abstract class BaseActivityPresenter<T : BaseActivityMvpView> : BaseBroadcastPresenter<T>() {

    private var mWorkoutStartedReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isAttachedToView) {
                if (shouldCloseOnWorkoutStart()) {
                    mvpView?.closeScreen()
                }
            }
        }
    }

    override fun init() {
        super.init()
        setupBroadcastReceivers()
    }

    override fun detachView() {
        mWorkoutStartedReceiver?.let { LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(it) }
        mWorkoutStartedReceiver = null
        super.detachView()
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // No-op
    }

    open fun onActivityResumed() {
        // No-op
    }

    open fun onActivityPaused() {
        // No-op
    }

    open fun onBackPressedConsumed(): Boolean {
        return false
    }

    protected open fun shouldCloseOnWorkoutStart(): Boolean {
        return false
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_WORKOUT_STARTED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mWorkoutStartedReceiver!!, filter)
    }
}