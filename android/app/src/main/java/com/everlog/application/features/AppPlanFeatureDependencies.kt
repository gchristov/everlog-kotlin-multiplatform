package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.PlanAnalytics
import com.everlog.android.plan.PlanFeatureDependencies
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.navigator.ELNavigator

class AppPlanFeatureDependencies : PlanFeatureDependencies {

    override fun planAnalytics(): PlanAnalytics = object : PlanAnalytics {
        private val analytics = AnalyticsManager.manager

        override fun created() = analytics.planCreated()

        override fun modified() = analytics.planModified()

        override fun deleted() = analytics.planDeleted()

        override fun started() = analytics.planStarted()

        override fun stopped() = analytics.planStopped()

        override fun weekAdded() = analytics.planWeekAdded()

        override fun weekDeleted() = analytics.planWeekDeleted()

        override fun daySetToTemplate() = analytics.planWeekDaySetRoutine()

        override fun daySetToRest() = analytics.planWeekDaySetRest()
    }

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
