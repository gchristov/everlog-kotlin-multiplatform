package com.everlog.application

import android.app.Application
import com.everlog.config.AppConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import java.lang.ref.WeakReference

class ELApplication : Application() {

    companion object {

        private var instance: WeakReference<ELApplication>? = null

        @JvmStatic
        fun getInstance(): ELApplication {
            return instance?.get()!!
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        AndroidThreeTen.init(this)
        AppConfig.configuration.configureApp()
    }
}