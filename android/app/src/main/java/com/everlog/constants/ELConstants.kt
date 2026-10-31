package com.everlog.constants

import com.everlog.BuildConfig
import com.everlog.android.exercise.ExerciseExtras
import com.everlog.android.exercise.picker.ExercisePickerActivity

object ELConstants {


    const val URL_TERMS = "https://www.termsfeed.com/terms-conditions/ab88f4b9d47638276ae7284d4a7ed3f8"
    const val URL_PRIVACY = "https://www.termsfeed.com/privacy-policy/68420a39232d3fc1b3e6d56275fed431"

    const val FIELD_CRASHLYTICS_USER_ID = "user_id"

    const val EXTRA_ROUTINE = "EXTRA_ROUTINE"
    const val EXTRA_WORKOUT = "EXTRA_WORKOUT"
    const val EXTRA_WORKOUT_JUST_FINISHED = "EXTRA_WORKOUT_JUST_FINISHED"
    const val EXTRA_WORKOUT_FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
    const val EXTRA_EXERCISE = "EXTRA_EXERCISE"
    // A new exercise's name to start from, e.g. what was searched for
    const val EXTRA_EXERCISE_NAME = ExerciseExtras.EXERCISE_NAME
    // The exercise just created, in the create screen's result
    const val EXTRA_EXERCISE_UUID = ExerciseExtras.EXERCISE_UUID
    const val EXTRA_EXERCISE_GROUPS = ExercisePickerActivity.EXTRA_EXERCISE_GROUPS
    const val EXTRA_WEB_URL = "EXTRA_WEB_URL"
    const val EXTRA_WEB_TITLE = "EXTRA_WEB_TITLE"
    const val EXTRA_SET_TYPE = "EXTRA_SET_TYPE"
    const val EXTRA_COVER_IMAGE = "EXTRA_COVER_IMAGE"
    const val EXTRA_VIEW_ONLY = "EXTRA_VIEW_ONLY"
    const val EXTRA_TYPE = "EXTRA_TYPE"
    const val EXTRA_SHOW_DETAILS_ON_SUCCESS = "EXTRA_SHOW_DETAILS_ON_SUCCESS"
    const val EXTRA_NUMBER_OF_ITEMS = "EXTRA_NUMBER_OF_ITEMS"
    const val EXTRA_INTEGRATION = "EXTRA_INTEGRATION"
    const val EXTRA_TITLE = "EXTRA_TITLE"


    const val FIREBASE_TEST_LAB_EMAIL = "hello@cloudtestlabaccounts.com"
    const val FIREBASE_TEST_LAB_PASSWORD = "KTI4We2uSTfl"


    const val ACTIVITY_RESULT_DELAY = 1000 // 100 ms

    const val ALPHA_PRO_FEATURE_DISABLED = 0.4f
}