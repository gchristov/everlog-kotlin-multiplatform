package com.everlog.android.plan.data

import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanState

// The plan the user is following and how far into it they are, kept on the device
interface PlanProgress {
    // Marks the plan's next day done, and says whether it did
    fun checkCompletedWorkout(workout: ELWorkout, performingFromPlan: Boolean): Boolean

    fun updateRoutineForPlan(context: Context, toSave: ELRoutine)

    fun deleteRoutineForPlan(context: Context, toSave: ELRoutine)

    fun isOngoing(plan: ELPlan?): Boolean

    fun hasOngoingPlan(): Boolean

    fun clearOngoingPlan()

    fun ongoingPlan(): ELPlan?

    fun ongoingPlanState(): ELPlanState?

    fun setOngoingPlan(plan: ELPlan?)

    fun setOngoingState(state: ELPlanState)
}
