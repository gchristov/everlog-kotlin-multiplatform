package com.everlog.android.plan.testfixtures

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanState

// The ongoing plan in memory: set [plan] to start one
class FakePlanProgress(var plan: ELPlan? = null) : PlanProgress {
    private var state: ELPlanState? = null

    override fun checkCompletedWorkout(workout: ELWorkout, performingFromPlan: Boolean) = false

    override fun updateRoutineForPlan(context: Context, toSave: ELRoutine) = Unit

    override fun deleteRoutineForPlan(context: Context, toSave: ELRoutine) = Unit

    override fun isOngoing(plan: ELPlan?) = plan != null && plan.uuid == this.plan?.uuid

    override fun hasOngoingPlan() = plan != null

    override fun clearOngoingPlan() {
        plan = null
        state = null
    }

    override fun ongoingPlan() = plan

    override fun ongoingPlanState() = state ?: plan?.let { ELPlanState() }

    override fun setOngoingPlan(plan: ELPlan?) {
        this.plan = plan
    }

    override fun setOngoingState(state: ELPlanState) {
        this.state = state
    }
}
