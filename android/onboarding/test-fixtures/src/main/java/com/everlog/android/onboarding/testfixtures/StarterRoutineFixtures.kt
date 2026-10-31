package com.everlog.android.onboarding.testfixtures

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.onboarding.data.StarterWeek

/**
 * The week's exercises as they'd come from the exercise library.
 */
fun exerciseLibrary(week: StarterWeek): List<ELExercise> = week.routines.flatMap { it.exercises }
    .distinctBy { it.exerciseId }
    .map { ELExercise(uuid = it.exerciseId) }
