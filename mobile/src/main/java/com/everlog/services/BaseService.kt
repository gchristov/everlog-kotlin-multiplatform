package com.everlog.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

abstract class BaseService : Service() {

    abstract fun tag(): String

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag()).i("Service started")
    }

    override fun onDestroy() {
        Timber.tag(tag()).i("Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}