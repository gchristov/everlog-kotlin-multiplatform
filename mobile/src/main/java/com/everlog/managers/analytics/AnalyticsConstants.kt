package com.everlog.managers.analytics

class AnalyticsConstants {

    companion object {

        // Screens

        const val SCREEN_SPLASH = "screen_splash"
        const val SCREEN_LOGIN = "screen_login"
        const val SCREEN_RESET_PASSWORD = "screen_reset_password"
        const val SCREEN_HOME = "screen_home"
        const val SCREEN_HOME_WEEK = "screen_week"
        const val SCREEN_HOME_WORKOUTS = "screen_workouts"
        const val SCREEN_HOME_ACTIVITY = "screen_activity"
        const val SCREEN_HOME_SETTINGS = "screen_settings"
        const val SCREEN_HOME_STATISTICS = "screen_statistics"
        const val SCREEN_HOME_HISTORY = "screen_history"
        const val SCREEN_EXERCISES = "screen_exercises"
        const val SCREEN_EXERCISE_PICKER = "screen_exercise_picker"
        const val SCREEN_EXERCISE_DETAILS = "screen_exercise_details"
        const val SCREEN_EXERCISE_CREATE = "screen_create_exercise"
        const val SCREEN_EXERCISE_GROUPS_CREATE = "screen_create_exercise_groups"
        const val SCREEN_EXERCISE_INFO = "screen_exercise_info"
        const val SCREEN_EXERCISE_STATISTICS = "screen_exercise_statistics"
        const val SCREEN_EXERCISE_HISTORY = "screen_exercise_history"
        const val SCREEN_ROUTINE_CREATE = "screen_create_routine"
        const val SCREEN_ROUTINE_DETAILS = "screen_routine_details"
        const val SCREEN_ROUTINE_PERFORM = "screen_perform_routine"
        const val SCREEN_ROUTINE_PICKER = "screen_routine_picker"
        const val SCREEN_SET_TYPE_PICKER = "screen_set_type_picker"
        const val SCREEN_WORKOUT = "screen_workout"
        const val SCREEN_WORKOUT_DETAILS = "screen_workout_details"
        const val SCREEN_WEB_VIEW = "screen_web_view"
        const val SCREEN_PRO = "screen_pro"
        const val SCREEN_COVER_IMAGE_PICKER = "screen_cover_image_picker"
        const val SCREEN_CONGRATULATE = "screen_congratulate"
        const val SCREEN_PLAN_CREATE = "screen_create_plan"
        const val SCREEN_PLAN_DETAILS = "screen_plan_details"
        const val SCREEN_MUSCLE_GOAL = "screen_settings_muscle_goals"
        const val SCREEN_INTEGRATION = "screen_integration"

        // Events

        const val EVENT_SCREEN_VIEW = "screen_view"

        // Remote Config

        const val EVENT_REMOTE_CONFIG_FETCHED = "remote_config_fetched"

        // Notifications

        const val EVENT_NOTIFICATION_HOME_DISMISSED = "notification_home_dismissed"

        // Rating

        const val EVENT_APP_RATE = "app_rate"
        const val EVENT_APP_STAR_RATING = "app_star_rating"
        const val EVENT_APP_FEEDBACK = "app_feedback"
        const val EVENT_APP_NOT_NOW = "app_not_now"

        // Login

        const val EVENT_USER_REGISTER = "user_registered"
        const val EVENT_USER_IDENTIFY = "user_identify"
        const val EVENT_USER_LOGOUT = "user_logout"

        // Exercises

        const val EVENT_EXERCISE_CREATED = "exercise_created"
        const val EVENT_EXERCISE_CREATED_SUGGESTION = "exercise_created_suggestion"
        const val EVENT_EXERCISE_MODIFIED = "exercise_modified"
        const val EVENT_EXERCISE_DELETED = "exercise_deleted"

        // Routines

        const val EVENT_ROUTINE_CREATED = "routine_created"
        const val EVENT_ROUTINE_MODIFIED = "routine_modified"
        const val EVENT_ROUTINE_DELETED = "routine_deleted"
        const val EVENT_ROUTINE_EXERCISE_GROUP_ADDED = "routine_exercise_group_added"
        const val EVENT_ROUTINE_EXERCISE_MODIFIED = "routine_exercise_modified"

        // Sets

        const val EVENT_SET_TYPE_SELECTED = "set_type_selected"
        const val EVENT_SET_WEIGHT_MODIFIED = "set_weight_modified"
        const val EVENT_SET_REQUIRED_REPS_MODIFIED = "set_required_reps_modified"
        const val EVENT_SET_REPS_MODIFIED = "set_reps_modified"
        const val EVENT_SET_REQUIRED_TIME_MODIFIED = "set_required_time_modified"
        const val EVENT_SET_TIME_MODIFIED = "set_time_modified"
        const val EVENT_SET_REST_TIME_MODIFIED = "set_rest_time_modified"
        const val EVENT_SET_MODIFIED = "set_modified"
        const val EVENT_SET_ADDED = "set_added"
        const val EVENT_SET_DELETED = "set_deleted"

        // Statistics

        const val EVENT_STATISTICS_RANGE_MODIFIED = "statistics_range_modified"

        // Settings

        const val EVENT_SETTINGS_MUSCLE_GOAL_MODIFIED = "settings_muscle_goal_modified"
        const val EVENT_SETTINGS_WEIGHT_MODIFIED = "settings_weight_modified"
        const val EVENT_SETTINGS_WEEKLY_GOAL_MODIFIED = "settings_weekly_goal_modified"
        const val EVENT_SETTINGS_REST_TIME_MODIFIED = "settings_rest_time_modified"
        const val EVENT_SETTINGS_WEIGHT_UNIT_MODIFIED = "settings_weight_unit_modified"
        const val EVENT_SETTINGS_FIRST_WEEK_DAY_MODIFIED = "settings_first_week_day_modified"

        // Workouts

        const val EVENT_WORKOUT_STARTED = "workout_started"
        const val EVENT_WORKOUT_QUICK_STARTED = "workout_quick_started"
        const val EVENT_WORKOUT_FROM_ROUTINE_STARTED = "workout_from_routine_started"
        const val EVENT_WORKOUT_STOPPED = "workout_stopped"
        const val EVENT_WORKOUT_COMPLETED = "workout_completed"
        const val EVENT_WORKOUT_NEXT_EXERCISE = "workout_next_exercise"
        const val EVENT_WORKOUT_PREV_EXERCISE = "workout_prev_exercise"
        const val EVENT_WORKOUT_REPS_MODIFIED = "workout_reps_modified"
        const val EVENT_WORKOUT_TIME_MODIFIED = "workout_time_modified"
        const val EVENT_WORKOUT_WEIGHT_MODIFIED = "workout_weight_modified"
        const val EVENT_WORKOUT_SERVICE_PREV_EXERCISE = "workout_service_prev_exercise"
        const val EVENT_WORKOUT_SERVICE_NEXT_EXERCISE = "workout_service_next_exercise"
        const val EVENT_WORKOUT_SERVICE_REPS_MODIFIED = "workout_service_reps_modified"
        const val EVENT_WORKOUT_SERVICE_WEIGHT_MODIFIED = "workout_service_weight_modified"
        const val EVENT_WORKOUT_SERVICE_WORKOUT_COMPLETED = "workout_service_workout_completed"
        const val EVENT_WORKOUT_CHANGE_MUSCLE_GOAL = "workout_change_muscle_goal"
        const val EVENT_WORKOUT_TIMER_STARTED_EXERCISE = "workout_timer_started_exercise"
        const val EVENT_WORKOUT_TIMER_STOPPED_EXERCISE = "workout_timer_stopped_exercise"
        const val EVENT_WORKOUT_TIMER_STARTED_REST = "workout_timer_started_rest"
        const val EVENT_WORKOUT_TIMER_STOPPED_REST = "workout_timer_stopped_rest"

        // Workout details

        const val EVENT_WORKOUT_DETAILS_NAME_MODIFIED = "workout_details_name_modified"
        const val EVENT_WORKOUT_DETAILS_DATE_MODIFIED = "workout_details_date_modified"
        const val EVENT_WORKOUT_DETAILS_NOTES_MODIFIED = "workout_details_notes_modified"
        const val EVENT_WORKOUT_DETAILS_EXERCISES_MODIFIED = "workout_details_exercises_modified"
        const val EVENT_WORKOUT_DETAILS_SHARED = "workout_details_shared"
        const val EVENT_WORKOUT_DETAILS_SAVED_AS_ROUTINE = "workout_details_saved_as_routine"
        const val EVENT_WORKOUT_DETAILS_DELETED = "workout_details_deleted"

        // Pro

        const val EVENT_PRO_BUY_MONTH = "pro_buy_month"
        const val EVENT_PRO_BUY_YEAR = "pro_buy_year"
        const val EVENT_PRO_BUY_PURCHASED = "pro_buy_purchased"
        const val EVENT_PRO_BUY_CANCELLED = "pro_buy_cancelled"
        const val EVENT_PRO_BUY_ALREADY_OWNED = "pro_buy_already_owned"
        const val EVENT_PRO_BUY_RESTORE = "pro_buy_restore"
        const val EVENT_PRO_MANAGE_SUBSCRIPTION = "pro_manage_subscription"

        // Plans

        const val EVENT_PLAN_CREATED = "plan_created"
        const val EVENT_PLAN_MODIFIED = "plan_modified"
        const val EVENT_PLAN_DELETED = "plan_deleted"
        const val EVENT_PLAN_WEEK_ADDED = "plan_week_added"
        const val EVENT_PLAN_WEEK_DELETED = "plan_week_deleted"
        const val EVENT_PLAN_WEEK_DAY_SET_ROUTINE = "plan_week_day_set_routine"
        const val EVENT_PLAN_WEEK_DAY_SET_REST = "plan_week_day_set_rest"
        const val EVENT_PLAN_STARTED = "plan_started"
        const val EVENT_PLAN_STOPPED = "plan_stopped"
        const val EVENT_PLAN_COMPLETED = "plan_completed"
        const val EVENT_PLAN_DAY_COMPLETE = "plan_day_complete"
        const val EVENT_PLAN_COVER_MODIFIED = "plan_cover_modified"

        // Integrations

        const val EVENT_INTEGRATION_ACCESS_REQUESTED = "integration_access_requested"
        const val EVENT_INTEGRATION_CONNECTED = "integration_connected"
        const val EVENT_INTEGRATION_DISCONNECTED = "integration_disconnected"
        const val EVENT_INTEGRATION_SYNC_REQUESTED = "integration_sync_requested"
        const val EVENT_INTEGRATION_UNSYNC_REQUESTED = "integration_unsync_requested"

        // Consent

        const val EVENT_CONSENT_NEWSLETTER_GRANTED = "consent_newsletter_granted"
        const val EVENT_CONSENT_NEWSLETTER_DENIED = "consent_newsletter_denied"

        // Properties

        const val PROPERTY_USER_ID = "userId"
        const val PROPERTY_USER_EMAIL = "email"
        const val PROPERTY_USER_DISPLAY_NAME = "displayName"
        const val PROPERTY_TYPE = "type"
        const val PROPERTY_VALUE = "value"
        const val PROPERTY_SCREEN_NAME = "screen_name"
    }
}