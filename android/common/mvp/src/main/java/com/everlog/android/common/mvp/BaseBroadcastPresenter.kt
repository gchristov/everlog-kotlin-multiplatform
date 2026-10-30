package com.everlog.android.common.mvp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

abstract class BaseBroadcastPresenter<T : BaseMvpView> : BasePresenter<T>() {

    private var mBroadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isAttachedToView) {
                when {
                    MvpBroadcasts.PREFERENCES_CHANGED == intent.action -> {
                        onPreferencesChanged()
                    }
                    MvpBroadcasts.REMOTE_CONFIG_REFRESHED == intent.action -> {
                        onRemoteConfigChanged()
                    }
                    MvpBroadcasts.PRO_CHANGED == intent.action -> {
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

    protected open fun onPreferencesChanged() {
        // No-op
    }

    protected open fun onRemoteConfigChanged() {
        // No-op
    }

    protected open fun onProChanged() {
        // No-op
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(MvpBroadcasts.PREFERENCES_CHANGED)
        filter.addAction(MvpBroadcasts.REMOTE_CONFIG_REFRESHED)
        filter.addAction(MvpBroadcasts.PRO_CHANGED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mBroadcastReceiver!!, filter)
    }
}
