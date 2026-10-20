package com.everlog.ui.activities.exercisepicker

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSetType

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

// The exercise as the rest of the app knows it
internal fun LibraryExercise.toELExercise() = ELExercise(
    uuid = uuid,
    name = name,
    category = category,
    splitSides = splitSides,
    createdDate = createdDate,
    imageUrl = imageUrl,
    createdByUserId = createdByUserId,
    privateToUser = own,
)
