package com.everlog.data.model.workout

import java.io.Serializable

data class ELWorkoutState (

        var groupIndex: Int = 0,
        var setIndex: Int = 0,
        var exerciseIndex: Int = 0,
        var exercisesInGroup: Int = 0

) : Serializable