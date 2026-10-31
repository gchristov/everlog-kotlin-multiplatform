package com.everlog.android.plan.data

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.template.data.session.OngoingPlanRepository

class RealOngoingPlanRepository(private val context: Context) : OngoingPlanRepository {

    override fun replaceRoutine(routine: ELRoutine) = PlanManager.manager.updateRoutineForPlan(context, routine)
}
