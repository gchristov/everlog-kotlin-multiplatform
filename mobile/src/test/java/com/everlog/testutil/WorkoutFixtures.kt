package com.everlog.testutil

import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Epoch millis for 9am on the given day, in the device time zone.
 */
fun at(year: Int, month: Int, day: Int): Long {
    return LocalDateTime.of(year, month, day, 9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * A set that was performed, as found in workout history.
 */
fun loggedSet(reps: Int, weightKg: Float) = ELSet(reps = reps, weight = weightKg)

/**
 * A set that hasn't been performed yet, as found in a workout that's just been started.
 */
fun plannedSet() = ELSet(requiredReps = 8)

/**
 * A workout with one exercise group per given exercise.
 */
fun workout(completedDate: Long, vararg exercises: Pair<ELExercise, List<ELSet>>): ELWorkout {
    val groups = exercises.map { (exercise, sets) ->
        ELExerciseGroup(exercises = mutableListOf(ELRoutineExercise(exercise.uuid, exercise, sets.toMutableList())))
    }
    return ELWorkout(routine = ELRoutine(exerciseGroups = groups.toMutableList()), completedDate = completedDate)
}
