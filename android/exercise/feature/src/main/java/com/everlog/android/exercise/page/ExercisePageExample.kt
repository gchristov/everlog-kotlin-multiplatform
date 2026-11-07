package com.everlog.android.exercise.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Sample sessions for an empty page's example, as a year of training could look: two or three sessions a week, the odd
 * week missed, a lighter warm-up set then working sets that tire, and a weight that goes up once its reps have climbed
 * from 6 to 10, with a lighter week every couple of months. Today has one, so every range shows some. The same every
 * time for the same [today], so the example doesn't change as the page redraws.
 *
 * @param workoutNames The workouts the sessions take turns being in, e.g. Chest, Upper body, Full body
 */
internal fun exampleSessions(today: LocalDate, workoutNames: List<String>, zone: ZoneId = ZoneId.systemDefault()): List<ExerciseSession> {
    val random = Random(today.toEpochDay())
    val days = trainingDays(today, random)
    return days.mapIndexed { index, day ->
        // 0 a year ago, 1 today
        val progress = 1f - day.until(today, ChronoUnit.DAYS).toFloat() / DaysInYear
        val week = day.toEpochDay() / DaysInWeek
        val lighterWeek = week % DeloadEveryWeeks == 0L
        val step = progress * Example.Steps
        val working = plates((Example.StartKg + Example.GainKg * step.toInt() / Example.Steps) * (if (lighterWeek) Example.Deload else 1f))
        val reps = if (lighterWeek) Example.MaxReps else Example.MinReps + ((Example.MaxReps - Example.MinReps) * (step % 1f)).roundToInt()
        ExerciseSession(
            workoutUuid = "example-$index",
            workoutName = workoutNames.getOrNull(index % workoutNames.size.coerceAtLeast(1)),
            completedDate = day.atTime(17 + random.nextInt(3), random.nextInt(60)).atZone(zone).toInstant().toEpochMilli(),
            exercise = ELRoutineExercise(Example.Uuid, ELExercise(uuid = Example.Uuid), sets(working, reps, random)),
        )
    }
}

// Back from today, a few days apart, now and then a week off
private fun trainingDays(today: LocalDate, random: Random): List<LocalDate> {
    val start = today.minusYears(1)
    return generateSequence(today) { day ->
        val gap = if (random.nextInt(Example.MissedWeekOneIn) == 0) Example.MissedWeekGap else Example.Gaps.random(random)
        day.minusDays(gap.toLong())
    }.takeWhile { !it.isBefore(start) }.toList()
}

// A warm-up at about two thirds, then two or three working sets losing a rep or two as they tire, the last sometimes
// a little heavier for fewer reps
private fun sets(working: Float, reps: Int, random: Random): MutableList<ELSet> {
    val sets = mutableListOf(ELSet(reps = 10 + random.nextInt(3), weight = plates(working * Example.WarmUp)))
    var setReps = (reps + random.nextInt(-1, 2)).coerceIn(Example.MinReps - 1, Example.MaxReps + 1)
    repeat(2 + random.nextInt(2)) {
        sets += ELSet(reps = setReps, weight = working)
        setReps = (setReps - random.nextInt(3)).coerceAtLeast(4)
    }
    if (random.nextInt(Example.TopSetOneIn) == 0) sets += ELSet(reps = 4 + random.nextInt(3), weight = working + Example.PlateKg)
    return sets
}

private fun plates(kg: Float): Float = (kg / Example.PlateKg).roundToInt() * Example.PlateKg

private const val DaysInYear = 365f
private const val DaysInWeek = 7L
private const val DeloadEveryWeeks = 9L

private object Example {
    const val Uuid = "example-exercise"
    val Gaps = listOf(2, 2, 3, 3, 4)
    const val MissedWeekOneIn = 12
    const val MissedWeekGap = 9
    const val StartKg = 50f
    const val GainKg = 30f
    const val Steps = 12
    const val MinReps = 6
    const val MaxReps = 10
    const val Deload = 0.85f
    const val WarmUp = 0.65f
    const val TopSetOneIn = 4
    const val PlateKg = 2.5f
}
