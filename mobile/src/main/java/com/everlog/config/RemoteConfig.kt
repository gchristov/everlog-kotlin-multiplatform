package com.everlog.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import java.io.Serializable

data class RemoteConfig (

        var notificationHome: HomeNotification? = null,
        var notificationAppUsage: AppUsageNotification? = null

) : Serializable {

    private val NOTIFICATION_HOME = "notification_home"
    private val NOTIFICATION_APP_USAGE = "notification_app_usage"

    fun populateFromSource(config: FirebaseRemoteConfig): RemoteConfig {
        notificationHome = getObjectFromRemoteConfig(NOTIFICATION_HOME, HomeNotification::class.java, config)
        notificationAppUsage = getObjectFromRemoteConfig(NOTIFICATION_APP_USAGE, AppUsageNotification::class.java, config)
        return this
    }

    private fun <T> getObjectFromRemoteConfig(key: String,
                                              clazz: Class<T>,
                                              config: FirebaseRemoteConfig): T {
        return Gson().fromJson(config.getString(key), clazz)
    }
}