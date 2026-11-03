package com.everlog.android.plan

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine

// The other screens the plan screens open, through the app's Navigator, and what they hand back
interface PlanNavigation {
    fun openHome(activity: Activity)

    fun openTemplateDetails(context: Context, routine: ELRoutine)

    fun openTemplatePicker(activity: Activity)

    // The template the template picker hands back, from onActivityResult
    fun pickedTemplate(requestCode: Int, resultCode: Int, data: Intent?): ELRoutine?

    fun openProBuy(context: Context)

    // Runs [block] for a Pro user, and offers Pro to anyone else
    fun runProFeature(context: Context, block: () -> Unit)
}
