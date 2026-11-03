package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.PlanNavigation
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppPlanNavigation : PlanNavigation {
    override fun openHome(activity: Activity) = ELNavigator(activity).openHome()

    override fun openTemplateDetails(context: Context, routine: ELRoutine) = ELNavigator(context).openRoutineDetails(routine, true)

    override fun openTemplatePicker(activity: Activity) = ELNavigator(activity).openRoutinePicker()

    override fun pickedTemplate(requestCode: Int, resultCode: Int, data: Intent?): ELRoutine? =
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_ROUTINE && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            data?.getSerializableExtra(ELConstants.EXTRA_ROUTINE) as? ELRoutine
        } else {
            null
        }

    override fun openProBuy(context: Context) = ELNavigator(context).openProBuy()

    override fun runProFeature(context: Context, block: () -> Unit) = ELNavigator(context).runProFeature(block)
}
