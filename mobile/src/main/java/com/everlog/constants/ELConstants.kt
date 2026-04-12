package com.everlog.constants

import com.everlog.BuildConfig

object ELConstants {

    const val SUPPORT_EMAIL = "support@everlogapp.com"

    const val URL_TERMS = "https://www.termsfeed.com/terms-conditions/ab88f4b9d47638276ae7284d4a7ed3f8"
    const val URL_PRIVACY = "https://www.termsfeed.com/privacy-policy/68420a39232d3fc1b3e6d56275fed431"
    const val URL_FACEBOOK = "https://www.facebook.com/everlogapp"
    const val URL_TWITTER = "https://twitter.com/everlogapp"
    const val URL_PING = "https://google.com"

    const val FIELD_CREATED_DATE = "createdDate"
    const val FIELD_CREATED_BY_USER_ID = "createdByUserId"
    const val FIELD_TYPE = "type"
    const val FIELD_NAME = "name"
    const val FIELD_UUID = "uuid"
    const val FIELD_CRASHLYTICS_USER_ID = "user_id"
    const val FIELD_CRASHLYTICS_USER_EMAIL = "user_email"
    const val FIELD_CRASHLYTICS_USER_NAME = "user_name"

    const val EXTRA_ROUTINE = "EXTRA_ROUTINE"
    const val EXTRA_WORKOUT = "EXTRA_WORKOUT"
    const val EXTRA_WORKOUT_STATE = "EXTRA_WORKOUT_STATE"
    const val EXTRA_WORKOUT_JUST_FINISHED = "EXTRA_WORKOUT_JUST_FINISHED"
    const val EXTRA_WORKOUT_FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
    const val EXTRA_EXERCISE = "EXTRA_EXERCISE"
    const val EXTRA_EXERCISE_GROUPS = "EXTRA_EXERCISE_GROUPS"
    const val EXTRA_WEB_URL = "EXTRA_WEB_URL"
    const val EXTRA_WEB_TITLE = "EXTRA_WEB_TITLE"
    const val EXTRA_SET_TYPE = "EXTRA_SET_TYPE"
    const val EXTRA_REST_TIMER_PROGRESS = "EXTRA_REST_TIMER_PROGRESS"
    const val EXTRA_REST_TIMER_REMAINING_SECONDS = "EXTRA_REST_TIMER_REMAINING_SECONDS"
    const val EXTRA_PLAN_UUID = "EXTRA_PLAN_UUID"
    const val EXTRA_COVER_IMAGE = "EXTRA_COVER_IMAGE"
    const val EXTRA_VIEW_ONLY = "EXTRA_VIEW_ONLY"
    const val EXTRA_TYPE = "EXTRA_TYPE"
    const val EXTRA_SHOW_DETAILS_ON_SUCCESS = "EXTRA_SHOW_DETAILS_ON_SUCCESS"
    const val EXTRA_NUMBER_OF_ITEMS = "EXTRA_NUMBER_OF_ITEMS"
    const val EXTRA_INTEGRATION = "EXTRA_INTEGRATION"
    const val EXTRA_SELECTION = "EXTRA_SELECTION"
    const val EXTRA_TITLE = "EXTRA_TITLE"
    const val EXTRA_SUBTITLE = "EXTRA_SUBTITLE"
    const val EXTRA_IMAGE = "EXTRA_IMAGE"

    const val BROADCAST_PREFERENCES_CHANGED = BuildConfig.APPLICATION_ID + ".BROADCAST_PREFERENCES_CHANGED"
    const val BROADCAST_PRO_CHANGED = BuildConfig.APPLICATION_ID + ".BROADCAST_PRO_CHANGED"
    const val BROADCAST_CURRENT_PLAN_STARTED = BuildConfig.APPLICATION_ID + ".BROADCAST_CURRENT_PLAN_STARTED"
    const val BROADCAST_CURRENT_PLAN_CHANGED = BuildConfig.APPLICATION_ID + ".BROADCAST_CURRENT_PLAN_CHANGED"
    const val BROADCAST_WORKOUT_STARTED = BuildConfig.APPLICATION_ID + ".BROADCAST_WORKOUT_STARTED"
    const val BROADCAST_REMOTE_CONFIG_REFRESHED = BuildConfig.APPLICATION_ID + ".BROADCAST_REMOTE_CONFIG_REFRESHED"

    const val CLOUD_TEST_EMAIL = "hello@cloudtestlabaccounts.com"
    const val CLOUD_TEST_PASSWORD = "KTI4We2uSTfl"

    const val NETWORK_TIMEOUT_INTERVAL: Long = 60 // 60 seconds
    const val NETWORK_PING_TIMEOUT_INTERVAL = 5000 // 5 seconds

    const val ACTIVITY_RESULT_DELAY = 1000 // 100 ms

    const val ALPHA_PRO_FEATURE_DISABLED = 0.4f
}