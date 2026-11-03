package com.everlog.android.template

import com.everlog.android.common.data.model.set.ELSetType

interface TemplateAnalytics {
    fun routineDeleted()
    fun setTypeSelected(type: ELSetType?)
}

// What the template screens are opened with and hand back. The app reads the same names.
object TemplateExtras {
    const val ROUTINE = "EXTRA_ROUTINE"
    const val VIEW_ONLY = "EXTRA_VIEW_ONLY"
    const val FROM_PLAN = "EXTRA_WORKOUT_FROM_PLAN"
    const val SET_TYPE = "EXTRA_SET_TYPE"
    const val EXERCISE_COUNT = "EXTRA_NUMBER_OF_ITEMS"
    const val SHOW_DETAILS_ON_SUCCESS = "EXTRA_SHOW_DETAILS_ON_SUCCESS"
}

object TemplateRequestCodes {
    const val PICK_ROUTINE = 2
    const val PICK_SET_TYPE = 3
    const val EDIT = 6
}

// The names analytics already has for the template screens
object TemplateScreenNames {
    const val DETAILS = "screen_routine_details"
    const val PERFORM = "screen_perform_routine"
    const val PICKER = "screen_routine_picker"
    const val SET_TYPE_PICKER = "screen_set_type_picker"
    const val EDITOR = "screen_create_routine_v2"
}
