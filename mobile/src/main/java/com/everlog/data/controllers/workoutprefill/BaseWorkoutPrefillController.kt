package com.everlog.data.controllers.workoutprefill

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import timber.log.Timber
import java.util.*

abstract class BaseWorkoutPrefillController {

    abstract fun tag(): String

    abstract fun prefill(stats: ExerciseStatsController.StatsResult,
                         setToPrefill: ELSet,
                         setIndex: Int)

    internal fun printExerciseMessage(setIndex: Int, message: String) {
        Timber.tag(tag()).d("[SET %s] %s",
                setIndex + 1, message)
    }

    internal fun printExerciseMessage(workout: ELWorkout,
                                      exercise: ELRoutineExercise,
                                      setIndex: Int, message: String) {
        Timber.tag(tag()).d("[%s] [%s] [%s] [SET %s] %s",
                workout.getName(), workout.getCompletedDateAsDate(), exercise.getName()?.uppercase(), setIndex + 1, message)
    }

    internal fun buildSetInfo(set: ELSet): String {
        return String.format("weight=%.2f, reps=%d", set.getWeight(), set.getReps())
    }

    class UnprefilledExercise {

        var group: ELExerciseGroup? = null
        var routineExercise: ELRoutineExercise? = null
        var exercise: ELExercise? = null
    }
}