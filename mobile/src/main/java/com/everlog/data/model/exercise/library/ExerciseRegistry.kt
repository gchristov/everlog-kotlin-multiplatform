package com.everlog.data.model.exercise.library

/**
 * The ids the exercise library uses, with their display names, from `global/exercises/registry/current`.
 * `tools/exercises/README.md` is the reference for the model.
 *
 * Every list is in the registry's order. Hidden entries are kept, as history and routines still
 * point at them, but [active] leaves them out of anything people pick from, like filters.
 */
data class ExerciseRegistry(
    val muscles: List<Muscle> = emptyList(),
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val positions: List<Entry> = emptyList(),
    val grips: List<Entry> = emptyList(),
    val families: List<Family> = emptyList(),
    val registryVersion: Int = 0,
) {
    data class Entry(
        val id: String,
        val name: String,
        val active: Boolean = true,
        // Other words to search by
        val aliases: List<String> = emptyList(),
    )

    data class Muscle(
        val entry: Entry,
        // A MuscleGroup id
        val group: String?,
        // "front" or "back"
        val side: String?,
    )

    data class MuscleGroup(
        val entry: Entry,
        val region: String?,
        // The legacy category (CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS) exercises in this group have
        val category: String?,
    )

    data class Equipment(
        val entry: Entry,
        // The main piece of an exercise, rather than an accessory like a bench
        val main: Boolean,
    )

    data class Family(
        val entry: Entry,
        val primaryMuscles: List<String>,
        // Moves like "curl to press"
        val combination: Boolean,
    )

    private val muscleById = muscles.associateBy { it.entry.id }
    private val groupByCategory = muscleGroups.filter { it.category != null }.associateBy { it.category }

    /**
     * A muscle's group, if the registry knows the muscle. Exercises can name muscles newer than the
     * app's copy of the registry.
     */
    fun groupOfMuscle(muscle: String): String? = muscleById[muscle]?.group

    // The muscle group for a legacy category, e.g. users' own exercises, which only have a category
    fun groupOfCategory(category: String?): String? = category?.let { groupByCategory[it]?.entry?.id }

    // A display name, or the id itself for ids newer than the app's copy of the registry
    fun muscleName(id: String): String = muscleById[id]?.entry?.name ?: id
    fun muscleGroupName(id: String): String = muscleGroups.nameOf(id) { it.entry }
    fun equipmentName(id: String): String = equipment.nameOf(id) { it.entry }
    fun positionName(id: String): String = positions.nameOf(id) { it }
    fun gripName(id: String): String = grips.nameOf(id) { it }
    fun familyName(id: String): String = families.nameOf(id) { it.entry }

    fun family(id: String?): Family? = id?.let { families.find { family -> family.entry.id == it } }

    private fun <T> List<T>.nameOf(id: String, entry: (T) -> Entry): String =
        firstOrNull { entry(it).id == id }?.let { entry(it).name } ?: id
}
