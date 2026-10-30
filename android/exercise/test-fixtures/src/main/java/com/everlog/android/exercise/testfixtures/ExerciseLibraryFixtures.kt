package com.everlog.android.exercise.testfixtures

import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import com.everlog.android.exercise.data.model.library.ExerciseRegistry
import com.everlog.android.exercise.data.model.library.LibraryExercise

/**
 * A small exercise library: chest and back, barbell and dumbbell (alias "db"), bench presses and
 * rows, and one of the user's own exercises with only a name and a category.
 */
object LibraryFixtures {
    val registry = ExerciseRegistry(
        muscles = listOf(
            ExerciseRegistry.Muscle(ExerciseRegistry.Entry("pecs", "Pecs"), group = "chest", side = "front"),
            ExerciseRegistry.Muscle(ExerciseRegistry.Entry("lats", "Lats"), group = "back", side = "back"),
        ),
        muscleGroups = listOf(
            ExerciseRegistry.MuscleGroup(ExerciseRegistry.Entry("chest", "Chest"), region = "upper", category = "CHEST"),
            ExerciseRegistry.MuscleGroup(ExerciseRegistry.Entry("back", "Back"), region = "upper", category = "BACK"),
            // Nothing uses it, so it's never offered as a filter
            ExerciseRegistry.MuscleGroup(ExerciseRegistry.Entry("legs", "Legs"), region = "lower", category = "LEGS"),
        ),
        equipment = listOf(
            ExerciseRegistry.Equipment(ExerciseRegistry.Entry("barbell", "Barbell"), main = true),
            ExerciseRegistry.Equipment(ExerciseRegistry.Entry("dumbbell", "Dumbbell", aliases = listOf("db")), main = true),
        ),
        positions = listOf(
            ExerciseRegistry.Entry("flat", "Flat"),
            ExerciseRegistry.Entry("incline", "Incline"),
        ),
        families = listOf(
            // Not in A to Z order, which the picker lists movements in
            ExerciseRegistry.Family(ExerciseRegistry.Entry("row", "Row"), primaryMuscles = listOf("lats"), combination = false),
            ExerciseRegistry.Family(ExerciseRegistry.Entry("bench_press", "Bench Press"), primaryMuscles = listOf("pecs"), combination = false),
        ),
    )

    val barbellBench = LibraryExercise(
        uuid = "bb_bench", name = "Barbell Bench Press", family = "bench_press", familyDefault = true,
        equipment = "barbell", positions = listOf("flat"), primaryMuscles = listOf("pecs"),
    )
    val dumbbellBench = LibraryExercise(
        uuid = "db_bench", name = "Dumbbell Bench Press", family = "bench_press",
        equipment = "dumbbell", positions = listOf("flat"), primaryMuscles = listOf("pecs"),
    )
    val inclineDumbbellBench = LibraryExercise(
        uuid = "incline_db_bench", name = "Incline Dumbbell Bench Press", family = "bench_press",
        equipment = "dumbbell", positions = listOf("incline"), primaryMuscles = listOf("pecs"),
    )
    val barbellRow = LibraryExercise(
        uuid = "bb_row", name = "Barbell Row", family = "row", familyDefault = true,
        equipment = "barbell", primaryMuscles = listOf("lats"), aliases = listOf("Bent-Over Row"),
    )
    val own = LibraryExercise(uuid = "own", name = "Zorbex Press", category = "CHEST", own = true)

    val global = listOf(barbellBench, dumbbellBench, inclineDumbbellBench, barbellRow)

    fun library(
        exercises: List<LibraryExercise> = global + own,
        templateCounts: Map<String, Int> = emptyMap(),
        lastDone: Map<String, Long> = emptyMap(),
    ) = ExerciseLibrary(exercises = exercises, registry = registry, templateCounts = templateCounts, lastDone = lastDone)
}
