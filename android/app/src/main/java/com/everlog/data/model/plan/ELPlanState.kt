package com.everlog.data.model.plan

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.data.model.workout.ELWorkout
import java.io.Serializable

data class ELPlanState (

        var totalWorkouts: Int = 0,
        var totalWeightKG: Float = 0f,
        var totalWorkoutTimeMS: Int = 0

) : Serializable {

    fun finishWorkout(workout: ELWorkout, unit: WeightUnit) {
        totalWorkouts++
        totalWeightKG += workout.getTotalWeight(unit)
        totalWorkoutTimeMS += workout.getDurationMillis().toInt()
    }
}