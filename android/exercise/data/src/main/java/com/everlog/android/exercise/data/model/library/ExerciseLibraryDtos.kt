package com.everlog.android.exercise.data.model.library

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

// The library's Firestore documents as stored, see tools/exercises/README.md. They live with the
// models because Firestore maps them by reflection, which proguard-rules.pro keeps for data.model. Fields the app doesn't
// know yet are ignored, so newer data still loads.

@IgnoreExtraProperties
data class LibraryExerciseDto(
    var uuid: String? = null,
    var name: String? = null,
    var status: String? = null,
    var family: String? = null,
    var familyDefault: Boolean = false,
    var equipment: String? = null,
    var accessories: List<String> = emptyList(),
    var position: List<String> = emptyList(),
    var grip: List<String> = emptyList(),
    var laterality: String? = null,
    var primaryMuscles: List<String> = emptyList(),
    var secondaryMuscles: List<String> = emptyList(),
    var kind: String? = null,
    var tracking: String? = null,
    var aliases: List<String> = emptyList(),
    var instructions: List<String> = emptyList(),
    var tips: List<String> = emptyList(),
    var category: String? = null,
    var imageUrl: String? = null,
    var media: MediaDto? = null,
    var splitSides: Boolean = false,
    var createdByUserId: String? = null,
    var createdDate: Long = 0,
) {
    // Not a stored field, so Firestore's mapper leaves it alone
    @get:Exclude
    val hidden get() = status == "hidden"

    // Null without a name, which every exercise should have
    fun toLibraryExercise(documentId: String, own: Boolean): LibraryExercise? = name?.let { name ->
        LibraryExercise(
            uuid = uuid ?: documentId,
            name = name,
            imageUrl = media?.image ?: imageUrl,
            family = family,
            familyDefault = familyDefault,
            equipment = equipment,
            accessories = accessories,
            positions = position,
            grips = grip,
            laterality = laterality,
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            kind = kind,
            tracking = tracking,
            aliases = aliases,
            instructions = instructions,
            tips = tips,
            category = category,
            own = own,
            splitSides = splitSides,
            createdByUserId = createdByUserId,
            createdDate = createdDate,
        )
    }
}

@IgnoreExtraProperties
data class MediaDto(
    var image: String? = null,
    var imageEnd: String? = null,
    var video: String? = null,
)

@IgnoreExtraProperties
data class ExerciseRegistryDto(
    var muscles: Map<String, RegistryEntryDto> = emptyMap(),
    var muscleGroups: Map<String, RegistryEntryDto> = emptyMap(),
    var equipment: Map<String, RegistryEntryDto> = emptyMap(),
    var positions: Map<String, RegistryEntryDto> = emptyMap(),
    var grips: Map<String, RegistryEntryDto> = emptyMap(),
    var families: Map<String, RegistryEntryDto> = emptyMap(),
    var registryVersion: Long = 0,
) {
    fun toRegistry() = ExerciseRegistry(
        muscles = muscles.ordered { id, dto -> ExerciseRegistry.Muscle(entry = dto.entry(id), group = dto.group, side = dto.side) },
        muscleGroups = muscleGroups.ordered { id, dto -> ExerciseRegistry.MuscleGroup(entry = dto.entry(id), region = dto.region, category = dto.category) },
        equipment = equipment.ordered { id, dto -> ExerciseRegistry.Equipment(entry = dto.entry(id), main = dto.kind == "main") },
        positions = positions.ordered { id, dto -> dto.entry(id) },
        grips = grips.ordered { id, dto -> dto.entry(id) },
        families = families.ordered { id, dto -> ExerciseRegistry.Family(entry = dto.entry(id), primaryMuscles = dto.primaryMuscles, combination = dto.combination) },
        registryVersion = registryVersion.toInt(),
    )

    // In the registry's order, then by id so it's stable
    private fun <T> Map<String, RegistryEntryDto>.ordered(map: (String, RegistryEntryDto) -> T): List<T> =
        entries.sortedWith(compareBy({ it.value.order }, { it.key })).map { (id, dto) -> map(id, dto) }
}

// One registry entry. Each list uses some of the fields: a muscle has a group, equipment a kind...
@IgnoreExtraProperties
data class RegistryEntryDto(
    var name: String? = null,
    var order: Long = Long.MAX_VALUE,
    var status: String? = null,
    var aliases: List<String> = emptyList(),
    var group: String? = null,
    var side: String? = null,
    var region: String? = null,
    var category: String? = null,
    var kind: String? = null,
    var primaryMuscles: List<String> = emptyList(),
    var combination: Boolean = false,
) {
    fun entry(id: String) = ExerciseRegistry.Entry(
        id = id,
        name = name ?: id,
        active = status != "hidden",
        aliases = aliases,
    )
}
