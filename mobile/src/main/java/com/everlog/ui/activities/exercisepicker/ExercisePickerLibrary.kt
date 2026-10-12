package com.everlog.ui.activities.exercisepicker

import androidx.annotation.StringRes
import com.everlog.R
import com.everlog.data.model.ELRoutine
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

// Prototype: the picker's own read of the library, straight from Firestore, and the rules from the
// spec on the Everlog canvas (Exercise Library page) for filtering, search and the list. The real
// data layer replaces the reading part.

internal enum class FilterKey(@StringRes val label: Int) {
    Muscle(R.string.exercise_picker_key_muscle),
    Equipment(R.string.exercise_picker_key_equipment),
    Movement(R.string.exercise_picker_key_movement),
    SetUp(R.string.exercise_picker_key_set_up),
    Grip(R.string.exercise_picker_key_grip),
}

internal data class FilterValue(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
)

// One filter in the search bar: "Equipment is Barbell, Dumbbell", or "is not" when [not]
internal data class FilterToken(
    val key: FilterKey,
    val values: List<String>,
    val not: Boolean = false,
)

internal data class PickerExercise(
    val uuid: String,
    val name: String,
    val imageUrl: String?,
    // Muscle groups of its primary muscles, or of its category for users' own exercises
    val groups: Set<String>,
    val equipment: String?,
    val family: String?,
    val positions: List<String>,
    val grips: List<String>,
    val aliases: List<String>,
    val familyDefault: Boolean,
    val own: Boolean,
    // How many of the user's workout templates use it
    val templates: Int,
) {
    fun values(key: FilterKey): Collection<String> = when (key) {
        FilterKey.Muscle -> groups
        FilterKey.Equipment -> listOfNotNull(equipment)
        FilterKey.Movement -> listOfNotNull(family)
        FilterKey.SetUp -> positions
        FilterKey.Grip -> grips
    }
}

internal data class PickerLibrary(
    val exercises: List<PickerExercise>,
    // Each key's values in the registry's order, only those some exercise uses
    val values: Map<FilterKey, List<FilterValue>>,
    // A family's muscle group (its first primary muscle's), to order families without a muscle filter
    val familyGroups: Map<String, String>,
) {
    private val names = values.mapValues { (_, values) -> values.associate { it.id to it.name } }

    fun valueName(key: FilterKey, id: String): String = names[key]?.get(id) ?: id

    fun valueOrder(key: FilterKey, id: String): Int = values[key]?.indexOfFirst { it.id == id }?.takeIf { it >= 0 } ?: Int.MAX_VALUE
}

private const val RegistryPath = "global/exercises/registry/current"

// Search words the registry doesn't have yet for muscle groups and equipment (families have theirs).
// The spec suggests moving these into the registry.
private val ExtraAliases = mapOf(
    FilterKey.Muscle to mapOf(
        "chest" to listOf("pecs"),
        "back" to listOf("lats"),
        "shoulders" to listOf("delts"),
        "arms" to listOf("biceps", "triceps"),
        "core" to listOf("abs"),
        "legs" to listOf("quads", "glutes", "hamstrings"),
    ),
    FilterKey.Equipment to mapOf(
        "barbell" to listOf("bb"),
        "dumbbell" to listOf("db"),
        "bodyweight" to listOf("bw"),
    ),
)

internal suspend fun loadPickerLibrary(): PickerLibrary = coroutineScope {
    val registryRead = async { FirebaseFirestore.getInstance().document(RegistryPath).get().await() }
    val globalRead = async { FirestorePathManager.globalExercisesCollection.get().await() }
    // The user's own exercises and templates are extras: the library still shows without them
    val ownRead = async { runCatching { FirestorePathManager.exercisesCollection.get().await() }.getOrNull() }
    val routinesRead = async { runCatching { FirestorePathManager.routinesCollection.get().await() }.getOrNull() }

    val registry = registryRead.await().data ?: emptyMap()
    val muscleGroupOf = registry.entries("muscles").associate { (id, entry) -> id to (entry["group"] as? String) }
    val groups = registry.entries("muscleGroups")
    val groupOfCategory = groups.associate { (id, entry) -> entry["category"] as? String to id }
    val families = registry.entries("families")

    val templates = HashMap<String, Int>()
    routinesRead.await()?.documents?.forEach { document ->
        val routine = runCatching { document.toObject(ELRoutine::class.java) }.getOrNull() ?: return@forEach
        routine.exerciseGroups
            .flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }
            .toSet()
            .forEach { templates[it] = (templates[it] ?: 0) + 1 }
    }

    val global = globalRead.await().documents
        .filter { it.getString("status") != "hidden" }
        .mapNotNull { it.toPickerExercise(muscleGroupOf, groupOfCategory, templates) }
    val own = ownRead.await()?.documents.orEmpty().mapNotNull { document ->
        val name = document.getString("name") ?: return@mapNotNull null
        val uuid = document.getString("uuid") ?: document.id
        PickerExercise(
            uuid = uuid,
            name = name,
            imageUrl = document.getString("imageUrl"),
            groups = setOfNotNull(groupOfCategory[document.getString("category")]),
            equipment = null,
            family = null,
            positions = emptyList(),
            grips = emptyList(),
            aliases = emptyList(),
            familyDefault = false,
            own = true,
            templates = templates[uuid] ?: 0,
        )
    }
    val exercises = global + own

    fun used(key: FilterKey, entries: List<Pair<String, Map<String, Any?>>>): List<FilterValue> {
        val ids = exercises.flatMap { it.values(key) }.toSet()
        return entries
            .filter { (id, _) -> id in ids }
            .map { (id, entry) ->
                @Suppress("UNCHECKED_CAST")
                val aliases = (entry["aliases"] as? List<String>).orEmpty() + ExtraAliases[key]?.get(id).orEmpty()
                FilterValue(id = id, name = entry["name"] as? String ?: id, aliases = aliases)
            }
    }

    PickerLibrary(
        exercises = exercises,
        values = mapOf(
            FilterKey.Muscle to used(FilterKey.Muscle, groups),
            FilterKey.Equipment to used(FilterKey.Equipment, registry.entries("equipment").filter { (_, entry) -> entry["kind"] == "main" }),
            FilterKey.Movement to used(FilterKey.Movement, families),
            FilterKey.SetUp to used(FilterKey.SetUp, registry.entries("positions")),
            FilterKey.Grip to used(FilterKey.Grip, registry.entries("grips")),
        ),
        familyGroups = families.mapNotNull { (id, entry) ->
            @Suppress("UNCHECKED_CAST")
            val muscle = (entry["primaryMuscles"] as? List<String>)?.firstOrNull()
            muscleGroupOf[muscle]?.let { id to it }
        }.toMap(),
    )
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toPickerExercise(
    muscleGroupOf: Map<String, String?>,
    groupOfCategory: Map<String?, String>,
    templates: Map<String, Int>,
): PickerExercise? {
    val name = getString("name") ?: return null
    val uuid = getString("uuid") ?: id
    val primary = (get("primaryMuscles") as? List<String>).orEmpty()
    val groups = primary.mapNotNull { muscleGroupOf[it] }.toSet()
        .ifEmpty { setOfNotNull(groupOfCategory[getString("category")]) }
    return PickerExercise(
        uuid = uuid,
        name = name,
        imageUrl = (get("media") as? Map<String, Any?>)?.get("image") as? String ?: getString("imageUrl"),
        groups = groups,
        equipment = getString("equipment"),
        family = getString("family"),
        positions = (get("position") as? List<String>).orEmpty(),
        grips = (get("grip") as? List<String>).orEmpty(),
        aliases = (get("aliases") as? List<String>).orEmpty(),
        familyDefault = getBoolean("familyDefault") == true,
        own = false,
        templates = templates[uuid] ?: 0,
    )
}

// A registry list's active entries in its order
@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.entries(name: String): List<Pair<String, Map<String, Any?>>> =
    (this[name] as? Map<String, Any?>).orEmpty()
        .mapNotNull { (id, value) -> (value as? Map<String, Any?>)?.let { id to it } }
        .filter { (_, entry) -> entry["status"] != "hidden" }
        .sortedBy { (_, entry) -> (entry["order"] as? Number)?.toInt() ?: Int.MAX_VALUE }

// Filtering and search

internal fun PickerExercise.matches(token: FilterToken): Boolean =
    values(token.key).any { it in token.values } != token.not

internal fun PickerExercise.matches(tokens: List<FilterToken>, skip: FilterKey? = null): Boolean =
    tokens.all { it.key == skip || matches(it) }

// Lower case, without accents or punctuation, split into words
internal fun searchWords(text: String): List<String> =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .split(" ")
        .filter { it.isNotEmpty() }

// Every word of the search starts a word of the exercise's name, its aliases, or the names and
// aliases of its family, equipment, muscle groups, set-up and grip, in any order: "press bench db"
// finds Dumbbell Bench Press. Returns how well it matches, lower is better, or null when it doesn't.
internal fun PickerExercise.searchScore(words: List<String>, library: PickerLibrary): Int? {
    if (words.isEmpty()) return 0
    val nameWords = searchWords(name)
    val haystack = nameWords +
        aliases.flatMap { searchWords(it) } +
        FilterKey.entries.flatMap { key ->
            values(key).flatMap { id ->
                library.values[key]?.find { it.id == id }
                    ?.let { value -> searchWords(value.name) + value.aliases.flatMap { searchWords(it) } }
                    .orEmpty()
            }
        }
    if (!words.all { word -> haystack.any { it.startsWith(word) } }) return null
    val query = words.joinToString(" ")
    return when {
        nameWords.joinToString(" ").startsWith(query) -> 0
        nameWords.any { it.startsWith(words.first()) } -> 1
        else -> 2
    }
}
