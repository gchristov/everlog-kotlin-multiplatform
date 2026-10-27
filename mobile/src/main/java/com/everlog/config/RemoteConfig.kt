package com.everlog.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import timber.log.Timber
import java.io.Serializable

data class RemoteConfig (

        var notificationHome: HomeNotification? = null,
        var notificationAppUsage: AppUsageNotification? = null,
        // The new exercise picker (TAS-451) instead of ExercisesActivity, for browsing and picking
        var exercisePickerV2: Boolean = false,
        // The exercise detail in Compose (TAS-452): for now its Info tab, with the library's instructions and tips
        var exerciseDetailsV2: Boolean = false,
        // The workout screen in Compose (TAS-455), for workouts started while it's on
        var workoutScreenV2: Boolean = false,
        // The Set suggestions screen in Compose instead of MuscleGoalActivity, from every entry point
        var setSuggestionsV2: Boolean = false,
        var feedbackForms: FeedbackForms? = null,
        // How old synced data may get before reading it asks the server again (DataSync); null for the default
        var dataSyncMaxAgeMinutes: Long? = null

) : Serializable {

    private val NOTIFICATION_HOME = "notification_home"
    private val NOTIFICATION_APP_USAGE = "notification_app_usage"
    private val EXERCISE_PICKER_V2 = "exercise_picker_v2"
    private val EXERCISE_DETAILS_V2 = "exercise_details_v2"
    private val WORKOUT_SCREEN_V2 = "workout_screen_v2"
    private val SET_SUGGESTIONS_V2 = "set_suggestions_v2"
    private val FEEDBACK_FORMS = "feedback_forms"
    private val DATA_SYNC_MAX_AGE_MINUTES = "data_sync_max_age_minutes"

    fun populateFromSource(config: FirebaseRemoteConfig): RemoteConfig {
        notificationHome = getObjectFromRemoteConfig(NOTIFICATION_HOME, HomeNotification::class.java, config)
        notificationAppUsage = getObjectFromRemoteConfig(NOTIFICATION_APP_USAGE, AppUsageNotification::class.java, config)
        exercisePickerV2 = config.getBoolean(EXERCISE_PICKER_V2)
        exerciseDetailsV2 = config.getBoolean(EXERCISE_DETAILS_V2)
        workoutScreenV2 = config.getBoolean(WORKOUT_SCREEN_V2)
        setSuggestionsV2 = config.getBoolean(SET_SUGGESTIONS_V2)
        feedbackForms = getObjectFromRemoteConfig(FEEDBACK_FORMS, FeedbackForms::class.java, config)
        // Without a value from the console or the defaults (which load asynchronously), getLong gives 0: always sync
        dataSyncMaxAgeMinutes = config.getValue(DATA_SYNC_MAX_AGE_MINUTES)
            .takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
            ?.asLong()
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