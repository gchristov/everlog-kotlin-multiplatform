package com.everlog.android.plan.data

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.template.data.session.OngoingPlanRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class RealOngoingPlanRepository(
    private val context: Context,
    private val planProgress: PlanProgress,
) : OngoingPlanRepository {

    override fun replaceRoutine(routine: ELRoutine) = planProgress.updateRoutineForPlan(context, routine)

    override fun removeRoutine(routine: ELRoutine) = planProgress.deleteRoutineForPlan(context, routine)
}
