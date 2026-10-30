package com.everlog.android.common.data.model.exercise

import com.everlog.android.common.data.model.workout.ELWorkout
import java.io.Serializable

data class ELExerciseHistory (

        var exercise: ELRoutineExercise? = null,
        var workout: ELWorkout? = null

) : Serializable