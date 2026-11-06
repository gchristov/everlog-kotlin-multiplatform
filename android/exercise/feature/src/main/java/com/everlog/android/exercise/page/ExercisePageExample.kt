package com.everlog.android.exercise.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * Sample sessions for an empty page's example: every other day of the year up to [today], today included, so every
 * range has some, with weights that slowly go up. The same every time for the same [today], so the example doesn't
 * change as the page redraws.
 */
internal fun exampleSessions(today: LocalDate, workoutName: String?, zone: ZoneId = ZoneId.systemDefault()): List<ExerciseSession> {
    val days = generateSequence(today) { it.minusDays(ExampleSizes.EveryDays) }.takeWhile { !it.isBefore(today.minusYears(1)) }.toList()
    return days.mapIndexed { index, day ->
        val progress = 1f - index.toFloat() / days.size
        val weight = Math.round((ExampleSizes.StartKg + ExampleSizes.GainKg * progress) / 2.5f) * 2.5f
        ExerciseSession(
            workoutUuid = "example-$index",
            workoutName = workoutName,
            completedDate = day.atTime(18, 0).atZone(zone).toInstant().toEpochMilli(),
            exercise = ELRoutineExercise(ExampleSizes.Uuid, ELExercise(uuid = ExampleSizes.Uuid), MutableList(3) { ELSet(reps = 8, weight = weight) }),
        )
    }
}

private object ExampleSizes {
    const val Uuid = "example-exercise"
    const val EveryDays = 2L
    const val StartKg = 60f
    const val GainKg = 20f
}
