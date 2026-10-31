package com.everlog.data.repositories

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.OngoingPlanRepository
import com.everlog.managers.PlanManager

class RealOngoingPlanRepository(private val context: Context) : OngoingPlanRepository {

    override fun replaceRoutine(routine: ELRoutine) = PlanManager.manager.updateRoutineForPlan(context, routine)
}
