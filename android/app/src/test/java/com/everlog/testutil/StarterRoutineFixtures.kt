package com.everlog.testutil

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.data.controllers.starterroutines.StarterWeek

/**
 * The week's exercises as they'd come from the exercise library.
 */
fun exerciseLibrary(week: StarterWeek): List<ELExercise> = week.routines.flatMap { it.exercises }
    .distinctBy { it.exerciseId }
    .map { ELExercise(uuid = it.exerciseId) }
