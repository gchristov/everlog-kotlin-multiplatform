package com.everlog.data.model.exercise.library

import com.everlog.android.common.data.model.exercise.ELExercise

/**
 * An exercise as the library describes it, with the fields `ELExercise` doesn't read: its family,
 * equipment, set-up, grip, muscles and so on. Ids point at the [ExerciseRegistry].
 * `tools/exercises/README.md` is the reference for what each field means.
 *
 * Users' own exercises ([own]) have only a name, an image and the legacy [category]; everything
 * else is empty.
 */
data class LibraryExercise(
    val uuid: String,
    val name: String,
    val imageUrl: String? = null,
    // Null when it has no family, or one the app's registry doesn't know yet
    val family: String? = null,
    val familyDefault: Boolean = false,
    // The main equipment, null for generic exercises (not the same as bodyweight)
    val equipment: String? = null,
    val accessories: List<String> = emptyList(),
    // The set-up, e.g. incline. Empty is the standard set-up
    val positions: List<String> = emptyList(),
    // Grip or stance. Empty is the standard grip
    val grips: List<String> = emptyList(),
    // both, alternating or single
    val laterality: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val kind: String? = null,
    val tracking: String? = null,
    val aliases: List<String> = emptyList(),
    // How to do it, one step each, and form tips, from the exercise pack
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val category: String? = null,
    val own: Boolean = false,
    // As stored, so the exercise can be handed to the rest of the app as an ELExercise
    val splitSides: Boolean = false,
    val createdByUserId: String? = null,
    val createdDate: Long = 0,
)

// The exercise as the rest of the app knows it
fun LibraryExercise.toELExercise() = ELExercise(
    uuid = uuid,
    name = name,
    category = category,
    splitSides = splitSides,
    createdDate = createdDate,
    imageUrl = imageUrl,
    createdByUserId = createdByUserId,
    privateToUser = own,
)
