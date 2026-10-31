package com.everlog.android.plan

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine

/**
 * What the plan feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [PlanFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface PlanFeatureDependencies {
    fun planAnalytics(): PlanAnalytics

    fun openHome(activity: Activity)

    fun openTemplateDetails(context: Context, routine: ELRoutine)

    fun openTemplatePicker(activity: Activity)

    // The template the template picker hands back, from onActivityResult
    fun pickedTemplate(requestCode: Int, resultCode: Int, data: Intent?): ELRoutine?

    fun openProBuy(context: Context)

    // Runs [block] for a Pro user, and offers Pro to anyone else
    fun runProFeature(context: Context, block: () -> Unit)
}

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

object PlanFeature {
    lateinit var dependencies: PlanFeatureDependencies
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
