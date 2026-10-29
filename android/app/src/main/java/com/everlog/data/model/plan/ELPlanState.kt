package com.everlog.data.model.plan

import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import java.io.Serializable

data class ELPlanState (

        var totalWorkouts: Int = 0,
        var totalWeightKG: Float = 0f,
        var totalWorkoutTimeMS: Int = 0

) : Serializable {

    fun finishWorkout(workout: ELWorkout) {
        totalWorkouts++
        totalWeightKG += workout.getTotalWeight()
        totalWorkoutTimeMS += workout.getDurationMillis().toInt()
    }
}