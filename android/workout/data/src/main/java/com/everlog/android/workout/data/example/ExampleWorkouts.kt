package com.everlog.android.workout.data.example

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import java.util.concurrent.TimeUnit
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * Sample workouts for an empty screen's example: Mondays, Wednesdays and Fridays for the year up to [until], taking
 * turns through [names] (e.g. Legs, Push, Pull), each with a few exercises of its muscles and weights that slowly go
 * up. The same every time for the same [until], so an example doesn't change as the screen redraws.
 */
fun exampleWorkouts(until: LocalDate, names: List<String?> = List(Days.size) { null }, zone: ZoneId = ZoneId.systemDefault()): List<ELWorkout> {
    val days = generateSequence(until.minusYears(1)) { it.plusDays(1) }
        .takeWhile { !it.isAfter(until) }
        .filter { it.dayOfWeek in TrainingDays }
        .toList()
    return days.mapIndexed { index, day ->
        val kind = index % Days.size
        val progress = index.toFloat() / days.size
        val started = day.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        val minutes = BaseMinutes + (index * 7) % MinutesSpread
        ELWorkout(
            uuid = "example-$index",
            routine = ELRoutine(name = names.getOrNull(kind), exerciseGroups = Days[kind].map { it.group(progress) }.toMutableList()),
            createdDate = started,
            completedDate = started + TimeUnit.MINUTES.toMillis(minutes.toLong()),
        )
    }
}

private class ExampleExercise(val uuid: String, val category: String, val startKg: Float, val gainKg: Float) {
    fun group(progress: Float): ELExerciseGroup {
        val weight = Math.round((startKg + gainKg * progress) / 2.5f) * 2.5f
        val sets = MutableList(3) { ELSet(reps = 8, weight = weight) }
        return ELExerciseGroup(exercises = mutableListOf(ELRoutineExercise(uuid, ELExercise(uuid = uuid, category = category), sets)))
    }
}

private val TrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

private val Days = listOf(
    listOf(
        ExampleExercise("example-squat", "LEGS", 80f, 20f),
        ExampleExercise("example-leg-curl", "LEGS", 35f, 10f),
        ExampleExercise("example-crunch", "ABS", 20f, 5f),
    ),
    listOf(
        ExampleExercise("example-bench", "CHEST", 60f, 15f),
        ExampleExercise("example-shoulder-press", "SHOULDERS", 30f, 7.5f),
        ExampleExercise("example-triceps", "ARMS", 20f, 5f),
    ),
    listOf(
        ExampleExercise("example-row", "BACK", 55f, 15f),
        ExampleExercise("example-pulldown", "BACK", 45f, 10f),
        ExampleExercise("example-curl", "ARMS", 12.5f, 5f),
    ),
)

private const val BaseMinutes = 48
private const val MinutesSpread = 22
