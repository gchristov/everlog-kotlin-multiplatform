package com.everlog.ui.activities.exercisepicker

import androidx.annotation.StringRes
import com.everlog.R
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.workout.ELWorkout
import com.google.firebase.firestore.Query
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

// Prototype: the picker's own read of the library, straight from Firestore, and the rules from the
// spec on the Everlog canvas (Exercise Library page) for filtering, search and the list. The real
// data layer replaces the reading part.

internal enum class FilterKey(@StringRes val label: Int) {
    Muscle(R.string.exercise_picker_key_muscle),
    Equipment(R.string.exercise_picker_key_equipment),
    // Only the user's own exercises: "Created by is Me"
    CreatedBy(R.string.exercise_picker_key_created_by),
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
    // When the user last did it, from their recent workouts
    val lastDone: Long? = null,
) {
    fun values(key: FilterKey): Collection<String> = when (key) {
        FilterKey.Muscle -> groups
        FilterKey.Equipment -> listOfNotNull(equipment)
        FilterKey.Movement -> listOfNotNull(family)
        FilterKey.SetUp -> positions
        FilterKey.Grip -> grips
        FilterKey.CreatedBy -> if (own) listOf(CreatedByMe) else emptyList()
    }
}

internal data class PickerLibrary(
    val exercises: List<PickerExercise>,
    // Each key's values in the registry's order, only those some exercise uses
    val values: Map<FilterKey, List<FilterValue>>,
    // A family's muscle group (its first primary muscle's), to order families without a muscle filter
    val familyGroups: Map<String, String>,
    // The exercises as the rest of the app knows them, by uuid, to open their details
    val details: Map<String, ELExercise> = emptyMap(),
    // What the user's own exercises need when they change: see withOwn
    val ownContext: OwnContext = OwnContext(),
) {
    data class OwnContext(
        val groupOfCategory: Map<String?, String> = emptyMap(),
        val templates: Map<String, Int> = emptyMap(),
        val lastDone: Map<String, Long> = emptyMap(),
        // The name of "Created by is Me"'s only value
        val me: FilterValue = FilterValue(CreatedByMe, CreatedByMe),
    )

    private val names = values.mapValues { (_, values) -> values.associate { it.id to it.name } }

    // "Me" keeps its name when the last of the user's exercises goes and the filter is still on
    fun valueName(key: FilterKey, id: String): String =
        names[key]?.get(id) ?: if (key == FilterKey.CreatedBy) ownContext.me.name else id

    class SearchEntry(val nameWords: List<String>, val words: List<String>)

    // Each exercise's words to search by, worked out once per library rather than on every search:
    // its name's, its aliases', and the names and aliases of its family, equipment, muscle groups,
    // set-up and grip
    private val searchIndex: Map<String, SearchEntry> by lazy {
        val valueWords = values.mapValues { (_, values) ->
            values.associate { value -> value.id to (searchWords(value.name) + value.aliases.flatMap { searchWords(it) }) }
        }
        exercises.associate { exercise ->
            val nameWords = searchWords(exercise.name)
            val words = nameWords +
                exercise.aliases.flatMap { searchWords(it) } +
                FilterKey.entries.flatMap { key -> exercise.values(key).flatMap { id -> valueWords[key]?.get(id).orEmpty() } }
            exercise.uuid to SearchEntry(nameWords = nameWords, words = words.distinct())
        }
    }

    fun searchEntry(exercise: PickerExercise): SearchEntry =
        searchIndex[exercise.uuid] ?: SearchEntry(searchWords(exercise.name), searchWords(exercise.name))

    fun valueOrder(key: FilterKey, id: String): Int = values[key]?.indexOfFirst { it.id == id }?.takeIf { it >= 0 } ?: Int.MAX_VALUE
}

private const val RegistryPath = "global/exercises/registry/current"

internal const val CreatedByMe = "me"

// How many of the latest workouts make up Recently done
private const val RecentWorkouts = 30L

// [me] names "Created by is Me"'s value, with its search words
internal suspend fun loadPickerLibrary(me: FilterValue): PickerLibrary = coroutineScope {
    val registryRead = async { FirebaseFirestore.getInstance().document(RegistryPath).get().await() }
    val globalRead = async { FirestorePathManager.globalExercisesCollection.get().await() }
    // The user's own exercises, templates and workouts are extras: the library still shows without them
    val ownRead = async { runCatching { FirestorePathManager.exercisesCollection.get().await() }.getOrNull() }
    val routinesRead = async { runCatching { FirestorePathManager.routinesCollection.get().await() }.getOrNull() }
    val workoutsRead = async {
        runCatching {
            FirestorePathManager.workoutsCollection
                .orderBy("createdDate", Query.Direction.DESCENDING)
                .limit(RecentWorkouts)
                .get().await()
        }.getOrNull()
    }

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

    // Newest first, so the first time an exercise shows up is the last time it was done
    val lastDone = HashMap<String, Long>()
    workoutsRead.await()?.documents?.forEach { document ->
        val workout = runCatching { document.toObject(ELWorkout::class.java) }.getOrNull() ?: return@forEach
        workout.routine?.exerciseGroups.orEmpty()
            .flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }
            .forEach { lastDone.putIfAbsent(it, workout.createdDate) }
    }

    val globalDocuments = globalRead.await().documents
    val details = globalDocuments.mapNotNull { document ->
        runCatching { document.toObject(ELExercise::class.java) }.getOrNull()?.let { exercise -> (exercise.uuid ?: document.id) to exercise }
    }.toMap()
    val global = globalDocuments
        .filter { it.getString("status") != "hidden" }
        .mapNotNull { it.toPickerExercise(muscleGroupOf, groupOfCategory, templates)?.let { exercise -> exercise.copy(lastDone = lastDone[exercise.uuid]) } }

    fun used(key: FilterKey, entries: List<Pair<String, Map<String, Any?>>>): List<FilterValue> {
        val ids = global.flatMap { it.values(key) }.toSet()
        return entries
            .filter { (id, _) -> id in ids }
            .map { (id, entry) ->
                @Suppress("UNCHECKED_CAST")
                val aliases = (entry["aliases"] as? List<String>).orEmpty()
                FilterValue(id = id, name = entry["name"] as? String ?: id, aliases = aliases)
            }
    }

    PickerLibrary(
        exercises = global,
        values = mapOf(
            // Every group, as users' own exercises can be in any of them
            FilterKey.Muscle to groups.map { (id, entry) ->
                @Suppress("UNCHECKED_CAST")
                FilterValue(id = id, name = entry["name"] as? String ?: id, aliases = (entry["aliases"] as? List<String>).orEmpty())
            },
            FilterKey.Equipment to used(FilterKey.Equipment, registry.entries("equipment").filter { (_, entry) -> entry["kind"] == "main" }),
            // A to Z: the registry's order for families is just the order they were added in
            FilterKey.Movement to used(FilterKey.Movement, families).sortedBy { it.name },
            FilterKey.SetUp to used(FilterKey.SetUp, registry.entries("positions")),
            FilterKey.Grip to used(FilterKey.Grip, registry.entries("grips")),
        ),
        familyGroups = families.mapNotNull { (id, entry) ->
            @Suppress("UNCHECKED_CAST")
            val muscle = (entry["primaryMuscles"] as? List<String>)?.firstOrNull()
            muscleGroupOf[muscle]?.let { id to it }
        }.toMap(),
        details = details,
        ownContext = PickerLibrary.OwnContext(groupOfCategory = groupOfCategory, templates = templates, lastDone = lastDone, me = me),
    ).withOwn(ownRead.await()?.documents.orEmpty())
}

/**
 * The library with the user's own exercises swapped for [documents], their latest version. "Created
 * by" only has a value when there are some.
 */
internal fun PickerLibrary.withOwn(documents: List<DocumentSnapshot>): PickerLibrary {
    val context = ownContext
    val own = documents.mapNotNull { document ->
        val name = document.getString("name") ?: return@mapNotNull null
        val uuid = document.getString("uuid") ?: document.id
        PickerExercise(
            uuid = uuid,
            name = name,
            imageUrl = document.getString("imageUrl"),
            groups = setOfNotNull(context.groupOfCategory[document.getString("category")]),
            equipment = null,
            family = null,
            positions = emptyList(),
            grips = emptyList(),
            aliases = emptyList(),
            familyDefault = false,
            own = true,
            templates = context.templates[uuid] ?: 0,
            lastDone = context.lastDone[uuid],
        )
    }
    val ownDetails = documents.mapNotNull { document ->
        runCatching { document.toObject(ELExercise::class.java) }.getOrNull()?.let { exercise -> (exercise.uuid ?: document.id) to exercise }
    }
    return copy(
        exercises = exercises.filter { !it.own } + own,
        values = values + (FilterKey.CreatedBy to if (own.isEmpty()) emptyList() else listOf(context.me)),
        details = details.filterKeys { uuid -> exercises.none { it.own && it.uuid == uuid } } + ownDetails,
    )
}

// The user's own exercises as they change, while collected: Firestore reports local writes at once
internal fun ownExerciseUpdates(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = FirestorePathManager.exercisesCollection.addSnapshotListener { snapshot, error ->
        if (snapshot != null && error == null) trySend(snapshot.documents)
    }
    awaitClose { registration.remove() }
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
    val entry = library.searchEntry(this)
    val nameWords = entry.nameWords
    val haystack = entry.words
    val exact = words.all { word -> haystack.any { it.startsWith(word) } }
    if (!exact && !words.all { word -> haystack.any { it.startsWith(word) || fuzzyStartsWith(it, word) } }) return null
    val query = words.joinToString(" ")
    return when {
        !exact -> 3
        nameWords.joinToString(" ").startsWith(query) -> 0
        nameWords.any { it.startsWith(words.first()) } -> 1
        else -> 2
    }
}

/**
 * Whether [candidate] starts with [word] give or take a typo: one for words of 4 to 6 letters, two
 * for longer ones, counting a wrong, missing, extra or swapped letter as one ("benhc" → bench,
 * "dumbel" → dumbbell). Shorter words must match exactly.
 */
internal fun fuzzyStartsWith(candidate: String, word: String): Boolean {
    val allowed = when {
        word.length >= 7 -> 2
        word.length >= 4 -> 1
        else -> return false
    }
    // The candidate's start can be shorter or longer than the word by the letters missing or extra
    return (word.length - allowed..word.length + allowed)
        .filter { it in 1..candidate.length }
        .any { length -> editDistance(word, candidate.take(length)) <= allowed }
}

// Optimal string alignment distance: insertions, deletions, substitutions and swaps of neighbours
private fun editDistance(a: String, b: String): Int {
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
            }
        }
    }
    return d[a.length][b.length]
}
