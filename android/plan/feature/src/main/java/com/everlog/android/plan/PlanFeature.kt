package com.everlog.android.plan


interface PlanAnalytics {
    fun created()
    fun modified()
    fun deleted()
    fun started()
    fun stopped()
    fun weekAdded()
    fun weekDeleted()
    fun daySetToTemplate()
    fun daySetToRest()
}

// What the plan screens are opened with and hand back. The app reads the same names.
object PlanExtras {
    const val PLAN_UUID = "EXTRA_PLAN_UUID"
    const val COVER_IMAGE = "EXTRA_COVER_IMAGE"
    const val CONGRATULATE_TYPE = "EXTRA_TYPE"
}

object PlanRequestCodes {
    const val PICK_COVER_IMAGE = 4
}

// The names analytics already has for the plan screens
object PlanScreenNames {
    const val CREATE = "screen_create_plan"
    const val DETAILS = "screen_plan_details"
    const val COVER_IMAGE_PICKER = "screen_cover_image_picker"
    const val CONGRATULATE = "screen_congratulate"
}
