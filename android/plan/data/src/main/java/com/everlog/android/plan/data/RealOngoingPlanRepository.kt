package com.everlog.android.plan.data

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.template.data.session.OngoingPlanRepository

class RealOngoingPlanRepository(
    private val context: Context,
    private val planProgress: PlanProgress,
) : OngoingPlanRepository {

    override fun replaceRoutine(routine: ELRoutine) = planProgress.updateRoutineForPlan(context, routine)
}
