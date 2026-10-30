package com.everlog.data.repositories

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.managers.PlanManager

/**
 * The plan the user is following, which is kept on the phone with a copy of each of its routines.
 */
interface OngoingPlanRepository {
    /**
     * Puts [routine] in place of the plan's copy of it, if there's an ongoing plan and the routine is in it.
     */
    fun replaceRoutine(routine: ELRoutine)
}

class RealOngoingPlanRepository(private val context: Context) : OngoingPlanRepository {

    override fun replaceRoutine(routine: ELRoutine) = PlanManager.manager.updateRoutineForPlan(context, routine)
}
