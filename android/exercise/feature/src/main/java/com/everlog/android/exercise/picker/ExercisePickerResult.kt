package com.everlog.android.exercise.picker

import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.data.model.library.toELExercise

/**
 * The picks as exercise groups: one per exercise, or one for all of them when they're linked as a superset or another
 * multi-exercise set.
 */
internal fun PickerResult.toExerciseGroups(): List<ELExerciseGroup> {
    if (setType != ELSetType.SINGLE) {
        val group = ELExerciseGroup.buildDefault(setType)
        exercises.forEach { group.exercises.add(ELRoutineExercise.buildRoutineExercise(it.toELExercise())) }
        return listOf(group)
    }
    return exercises.map { exercise ->
        ELExerciseGroup.buildDefault(setType).apply { this.exercises.add(ELRoutineExercise.buildRoutineExercise(exercise.toELExercise())) }
    }
}
