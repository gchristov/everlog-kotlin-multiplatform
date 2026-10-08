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
        const val SCREEN_ONBOARDING_WELCOME = "screen_onboarding_welcome"
        const val SCREEN_ONBOARDING_QUESTIONS = "screen_onboarding_questions"
        const val SCREEN_ONBOARDING_BUILDING = "screen_onboarding_building"
        const val SCREEN_ONBOARDING_REVEAL = "screen_onboarding_reveal"
        const val SCREEN_ONBOARDING_SAVING = "screen_onboarding_saving"

        // Events

        const val EVENT_SCREEN_VIEW = "screen_view"

        // Remote Config

        const val EVENT_REMOTE_CONFIG_FETCHED = "remote_config_fetched"

        // Notifications

        const val EVENT_NOTIFICATION_HOME_SHOWN = "notification_home_shown"
        const val EVENT_NOTIFICATION_HOME_TAPPED = "notification_home_tapped"
        const val EVENT_NOTIFICATION_HOME_DISMISSED = "notification_home_dismissed"
        const val EVENT_APP_USAGE_REMINDER_SHOWN = "app_usage_reminder_shown"
        const val EVENT_APP_USAGE_REMINDER_OPENED = "app_usage_reminder_opened"

        // Rating

        const val EVENT_APP_RATE_PROMPT_TRIGGERED = "app_rate_prompt_triggered"

        // Login

        const val EVENT_USER_REGISTER = "user_registered"
        const val EVENT_USER_LOGIN = "user_logged_in"
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
        const val EVENT_SET_EDITED = "set_edited"
        const val EVENT_SET_ADDED = "set_added"
        const val EVENT_SET_DELETED = "set_deleted"
        const val EVENT_SET_COMPLETED = "set_completed"
        const val EVENT_SET_GROUP_COMPLETED = "set_group_completed"

        // Statistics

        const val EVENT_STATISTICS_RANGE_MODIFIED = "statistics_range_modified"

        // Settings

        const val EVENT_SETTINGS_MUSCLE_GOAL_MODIFIED = "settings_muscle_goal_modified"
        const val EVENT_SETTINGS_WEIGHT_MODIFIED = "settings_weight_modified"
        const val EVENT_SETTINGS_WEEKLY_GOAL_MODIFIED = "settings_weekly_goal_modified"
        const val EVENT_SETTINGS_REST_TIME_MODIFIED = "settings_rest_time_modified"
        const val EVENT_SETTINGS_WEIGHT_UNIT_MODIFIED = "settings_weight_unit_modified"
        const val EVENT_SETTINGS_FIRST_WEEK_DAY_MODIFIED = "settings_first_week_day_modified"
        const val EVENT_SETTINGS_KEEP_SCREEN_ON_MODIFIED = "settings_keep_screen_on_modified"

        // Home

        const val EVENT_HOME_ADD_FAB_TAPPED = "home_add_fab_tapped"
        const val EVENT_HOME_ADD_WEEK_EMPTY_STATE_TAPPED = "home_add_week_empty_state_tapped"

        // App update

        const val EVENT_APP_UPDATE_PROMPT_SHOWN = "app_update_prompt_shown"
        const val EVENT_APP_UPDATE_ACCEPTED = "app_update_accepted"
        const val EVENT_APP_UPDATE_DECLINED = "app_update_declined"
        const val EVENT_APP_UPDATE_DOWNLOADED = "app_update_downloaded"
        const val EVENT_APP_UPDATE_RESTART_TAPPED = "app_update_restart_tapped"
        const val EVENT_APP_UPDATE_FAILED = "app_update_failed"

        // Workouts

        const val EVENT_WORKOUT_STARTED = "workout_started"
        const val EVENT_WORKOUT_STOPPED = "workout_stopped"
        const val EVENT_WORKOUT_COMPLETED = "workout_completed"
        const val EVENT_WORKOUT_DISCARD_PROMPT_SHOWN = "workout_discard_prompt_shown"
        const val EVENT_WORKOUT_DISCARD_PROMPT_FINISHED = "workout_discard_prompt_finished"
        const val EVENT_WORKOUT_DISCARD_PROMPT_CANCELLED = "workout_discard_prompt_cancelled"
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
        const val EVENT_WORKOUT_SERVICE_TIMER_STARTED_EXERCISE = "workout_service_timer_started_exercise"
        const val EVENT_WORKOUT_SERVICE_TIMER_STOPPED_EXERCISE = "workout_service_timer_stopped_exercise"
        const val EVENT_WORKOUT_SERVICE_TIMER_STOPPED_REST = "workout_service_timer_stopped_rest"
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

        // Onboarding

        const val EVENT_ONBOARDING_STEP_VIEWED = "onboarding_step_viewed"
        const val EVENT_ONBOARDING_STEP_COMPLETED = "onboarding_step_completed"
        const val EVENT_ONBOARDING_STEP_SKIPPED = "onboarding_step_skipped"
        const val EVENT_ONBOARDING_STEP_FAILED = "onboarding_step_failed"
        const val EVENT_ONBOARDING_STEP_RETRIED = "onboarding_step_retried"
        const val EVENT_ONBOARDING_QUESTION_REOPENED = "onboarding_question_reopened"
        const val EVENT_ONBOARDING_QUESTION_EDITED = "onboarding_question_edited"
        const val EVENT_ONBOARDING_SKIP_PROMPT_SHOWN = "onboarding_skip_prompt_shown"
        const val EVENT_ONBOARDING_SKIP_PROMPT_CANCELLED = "onboarding_skip_prompt_cancelled"
        const val EVENT_ONBOARDING_TEMPLATE_TOGGLED = "onboarding_template_toggled"
        const val EVENT_ONBOARDING_FINISHED = "onboarding_finished"

        // Properties

        const val PROPERTY_USER_ID = "userId"
        const val PROPERTY_TYPE = "type"
        const val PROPERTY_VALUE = "value"
        const val PROPERTY_TITLE = "title"
        const val PROPERTY_ATTEMPT = "attempt"
        const val PROPERTY_SOURCE = "source"
        const val PROPERTY_PROMPT_NUMBER = "prompt_number"
        const val PROPERTY_METHOD = "method"
        const val PROPERTY_SET_TYPE = "set_type"
        const val PROPERTY_TIMED = "timed"
        const val PROPERTY_SETS_COMPLETED = "sets_completed"
        const val PROPERTY_EXERCISES = "exercises"
        const val PROPERTY_STEP = "step"
        const val PROPERTY_OUTCOME = "outcome"
        const val PROPERTY_ROUTINES = "routines"
        const val PROPERTY_OPEN = "open"

        // Legacy user properties. Versions before 2.11.0 set the user's email and name as user
        // properties, which Firebase keeps on the device until cleared. They're PII, so they are
        // cleared on every start. Never set them again.

        const val LEGACY_PROPERTY_EMAIL = "email"
        const val LEGACY_PROPERTY_DISPLAY_NAME = "displayName"

        // Values

        const val LOGIN_METHOD_GUEST = "guest"
        const val LOGIN_METHOD_EMAIL = "email"
        const val LOGIN_METHOD_GOOGLE = "google"

        const val WORKOUT_SOURCE_QUICK = "quick"
        const val WORKOUT_SOURCE_PLAN = "plan"
        const val WORKOUT_SOURCE_ROUTINE = "routine"

        const val SET_COMPLETED_SOURCE_SCREEN = "screen"
        const val SET_COMPLETED_SOURCE_NOTIFICATION = "notification"

        // Where the prompt to discard an ongoing workout was shown: leaving the workout screen, or the
        // home screen's resume prompt after the app was closed mid-workout
        const val DISCARD_PROMPT_SOURCE_WORKOUT = "workout"
        const val DISCARD_PROMPT_SOURCE_HOME = "home"

        // Onboarding steps, besides the questions, which are their question's id (e.g. units, days)
        const val ONBOARDING_STEP_WELCOME = "welcome"
        const val ONBOARDING_STEP_BUILDING = "building"
        const val ONBOARDING_STEP_REVEAL = "reveal"
        const val ONBOARDING_STEP_SAVING = "saving"

        // The reveal's buttons, as its step's completed value
        const val ONBOARDING_REVEAL_LOOKS_GOOD = "looks_good"
        const val ONBOARDING_REVEAL_BUILD_OWN = "build_own"

        // How the onboarding ended: the starter templates saved, the user's own template saved, or
        // skipped (Skip setup, or Skip after building or saving failed)
        const val ONBOARDING_OUTCOME_STARTER_TEMPLATES = "starter_templates"
        const val ONBOARDING_OUTCOME_OWN_TEMPLATE = "own_template"
        const val ONBOARDING_OUTCOME_SKIPPED = "skipped"
    }
}