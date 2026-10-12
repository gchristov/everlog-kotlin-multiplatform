package com.everlog.data.model.exercise.library

/**
 * The exercise library as a screen shows it: the global exercises (without hidden ones), then the
 * user's own, with the registry to name and group them and how often the user's templates use each.
 */
data class ExerciseLibrary(
    val exercises: List<LibraryExercise>,
    val registry: ExerciseRegistry,
    // How many of the user's workout templates use each exercise, by uuid
    val templateCounts: Map<String, Int> = emptyMap(),
) {
    /**
     * The muscle groups an exercise works: its primary muscles' groups, or its category's group
     * when it has no muscles the registry knows, like users' own exercises.
     */
    fun muscleGroups(exercise: LibraryExercise): Set<String> =
        exercise.primaryMuscles.mapNotNull { registry.groupOfMuscle(it) }.toSet()
            .ifEmpty { setOfNotNull(registry.groupOfCategory(exercise.category)) }

    fun templateCount(exercise: LibraryExercise): Int = templateCounts[exercise.uuid] ?: 0
}
