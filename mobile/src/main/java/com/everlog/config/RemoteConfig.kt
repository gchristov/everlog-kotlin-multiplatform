package com.everlog.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import timber.log.Timber
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
                                              config: FirebaseRemoteConfig): T? {
        return parse(config.getString(key), clazz)
    }

    companion object {

        /**
         * A typo in the Remote Config console (e.g. a string where a number is expected) is dropped
         * instead of crashing the fetch callback.
         */
        fun <T> parse(json: String, clazz: Class<T>): T? {
            return try {
                Gson().fromJson(json, clazz)
            } catch (e: Exception) {
                Timber.tag("RemoteConfig").e(e, "Invalid remote config: %s", json)
                null
            }
        }
    }
}