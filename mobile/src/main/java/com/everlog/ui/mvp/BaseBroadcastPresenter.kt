package com.everlog.ui.mvp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELConstants

abstract class BaseBroadcastPresenter<T : BaseMvpView> : BasePresenter<T>() {

    private var mBroadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isAttachedToView) {
                when {
                    ELConstants.BROADCAST_PREFERENCES_CHANGED == intent.action -> {
                        onPreferencesChanged()
                    }
                    ELConstants.BROADCAST_REMOTE_CONFIG_REFRESHED == intent.action -> {
                        onRemoteConfigChanged()
                    }
                    ELConstants.BROADCAST_PRO_CHANGED == intent.action -> {
                        onProChanged()
                    }
                }
            }
        }
    }

    override fun init() {
        super.init()
        setupBroadcastReceivers()
    }

    override fun detachView() {
        mBroadcastReceiver?.let { LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(it) }
        mBroadcastReceiver = null
        super.detachView()
    }

    internal open fun onPreferencesChanged() {
        // No-op
    }

    internal open fun onRemoteConfigChanged() {
        // No-op
    }

    internal open fun onProChanged() {
        // No-op
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_PREFERENCES_CHANGED)
        filter.addAction(ELConstants.BROADCAST_REMOTE_CONFIG_REFRESHED)
        filter.addAction(ELConstants.BROADCAST_PRO_CHANGED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mBroadcastReceiver!!, filter)
    }
}
