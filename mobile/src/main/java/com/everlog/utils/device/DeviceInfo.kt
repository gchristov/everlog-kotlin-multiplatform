package com.everlog.utils.device

import android.content.Context
import android.os.Build
import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.application.ELApplication
import java.util.*

class DeviceInfo private constructor(builder: Builder) {

    private val model: String?
    private val androidVersion: String?
    private val appVersion: String?

    init {
        model = builder.model
        androidVersion = builder.androidVersion
        appVersion = builder.application
    }

    val deviceInfo: String
        get() = String.format(Locale.getDefault(),
                "Device: %s / Android: %s / App: %s",
                model, androidVersion, appVersion)

    val appInfo: String
        get() = String.format(Locale.getDefault(),
                "%s %s (%d)",
                ELApplication.getInstance().getString(R.string.everlog_app_name), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    class Builder {

        internal var model: String? = null
        internal var androidVersion: String? = null
        internal var application: String? = null

        init {
            model = deviceModel
            androidVersion = getAndroidVersion()
            application = getApplication()
        }

        fun build(): DeviceInfo {
            return DeviceInfo(this)
        }

        private val deviceModel: String
            get() = String.format("%s %s", Build.MANUFACTURER, Build.MODEL)

        private fun getAndroidVersion(): String {
            return Build.VERSION.SDK_INT.toString()
        }

        private fun getApplication(): String {
            return String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        }
    }
}