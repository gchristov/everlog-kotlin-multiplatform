package com.everlog.constants

import com.everlog.android.auth.AuthRequestCodes
import com.everlog.android.exercise.create.CreateExerciseActivity
import com.everlog.android.template.TemplateRequestCodes
object ELActivityRequestCodes {

    const val REQUEST_PICK_EXERCISES = 1
    const val REQUEST_PICK_ROUTINE = TemplateRequestCodes.PICK_ROUTINE
    const val REQUEST_PICK_SET_TYPE = TemplateRequestCodes.PICK_SET_TYPE
    const val REQUEST_PICK_COVER_IMAGE = 4
    const val REQUEST_EDIT_EXERCISE = CreateExerciseActivity.REQUEST_CODE
    const val REQUEST_EDIT_ROUTINE = TemplateRequestCodes.EDIT
    const val REQUEST_EDIT_EXERCISE_GROUPS = 7
    const val REQUEST_LOGIN_GOOGLE = 8
    const val REQUEST_ONBOARDING = AuthRequestCodes.ONBOARDING
}