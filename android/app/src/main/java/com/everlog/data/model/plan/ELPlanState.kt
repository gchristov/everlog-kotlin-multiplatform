package com.everlog.data.model.plan

import com.everlog.android.common.data.model.workout.ELWorkout
import java.io.Serializable

data class ELPlanState (

        var totalWorkouts: Int = 0,
        var totalWorkoutTimeMS: Int = 0

) : Serializable {

    fun finishWorkout(workout: ELWorkout) {
        totalWorkouts++
        totalWorkoutTimeMS += workout.getDurationMillis().toInt()
    }
}
