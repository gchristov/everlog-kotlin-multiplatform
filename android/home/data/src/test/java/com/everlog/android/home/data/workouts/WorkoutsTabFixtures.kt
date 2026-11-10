package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout

internal fun template(uuid: String, name: String = uuid, createdDate: Long = 0) = ELRoutine(uuid = uuid, name = name, createdDate = createdDate)

// A workout started from each template in turn, a day apart
internal fun workoutsFrom(vararg templates: String): List<ELWorkout> = templates.mapIndexed { day, uuid ->
    ELWorkout(uuid = "w$day", routine = template(uuid), createdDate = day * DayMillis, fromRoutine = true)
}

internal fun history(vararg templates: String) = templateHistory(workoutsFrom(*templates))

internal const val DayMillis = 86_400_000L
