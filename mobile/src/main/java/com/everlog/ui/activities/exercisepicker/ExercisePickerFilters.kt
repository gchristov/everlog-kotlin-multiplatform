package com.everlog.ui.activities.exercisepicker

import androidx.annotation.StringRes
import com.everlog.R
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.LibraryExercise
import java.text.Normalizer

// The picker's filters and search over the exercise library. The spec is on the Everlog canvas
// (Exercise Library page): filters are tokens like "Equipment is Barbell, Dumbbell"; values in one
// token match any of them, tokens all apply, and "is not" matches none of the values.

internal enum class FilterKey(@StringRes val label: Int, val analyticsName: String) {
    Muscle(R.string.exercise_picker_key_muscle, "muscle"),
    Equipment(R.string.exercise_picker_key_equipment, "equipment"),
    // Only the user's own exercises: "Created by is Me"
    CreatedBy(R.string.exercise_picker_key_created_by, "created_by"),
    Movement(R.string.exercise_picker_key_movement, "movement"),
    SetUp(R.string.exercise_picker_key_set_up, "set_up"),
    Grip(R.string.exercise_picker_key_grip, "grip"),
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

internal const val CreatedByMe = "me"

/**
 * The library ready to filter and search: each key's values, each exercise's values and search words.
 * Built off the main thread, once per library (it changes when the user's own exercises do).
 *
 * @param me Created by's only value, with its name and search words.
 */
internal class PickerIndex(val library: ExerciseLibrary, me: FilterValue) {
    val exercises: List<LibraryExercise> = library.exercises

    private val groups: Map<String, Set<String>> = exercises.associate { it.uuid to library.muscleGroups(it) }

    fun values(exercise: LibraryExercise, key: FilterKey): Collection<String> = when (key) {
        FilterKey.Muscle -> groups[exercise.uuid].orEmpty()
        FilterKey.Equipment -> listOfNotNull(exercise.equipment)
        FilterKey.CreatedBy -> if (exercise.own) listOf(CreatedByMe) else emptyList()
        FilterKey.Movement -> listOfNotNull(exercise.family)
        FilterKey.SetUp -> exercise.positions
        FilterKey.Grip -> exercise.grips
    }

    // Each key's values in the registry's order (movements A to Z), only active ones some exercise
    // uses, so a filter never offers nothing. Unknown ids stay out, as the library's README asks.
    val filterValues: Map<FilterKey, List<FilterValue>> = run {
        val registry = library.registry
        fun used(key: FilterKey) = exercises.flatMap { values(it, key) }.toSet()
        val usedGroups = used(FilterKey.Muscle)
        val usedEquipment = used(FilterKey.Equipment)
        val usedFamilies = used(FilterKey.Movement)
        val usedPositions = used(FilterKey.SetUp)
        val usedGrips = used(FilterKey.Grip)
        mapOf(
            FilterKey.Muscle to registry.muscleGroups.map { it.entry }.filter { it.active && it.id in usedGroups }.map { it.toValue() },
            FilterKey.Equipment to registry.equipment.filter { it.main && it.entry.active && it.entry.id in usedEquipment }.map { it.entry.toValue() },
            FilterKey.CreatedBy to if (exercises.any { it.own }) listOf(me) else emptyList(),
            // A to Z: the registry's order for families is just the order they were added in
            FilterKey.Movement to registry.families.map { it.entry }.filter { it.active && it.id in usedFamilies }.map { it.toValue() }.sortedBy { it.name },
            FilterKey.SetUp to registry.positions.filter { it.active && it.id in usedPositions }.map { it.toValue() },
            FilterKey.Grip to registry.grips.filter { it.active && it.id in usedGrips }.map { it.toValue() },
        )
    }

    private val names = filterValues.mapValues { (_, values) -> values.associate { it.id to it.name } }
    private val meName = me.name

    // A value's name, or its id when the registry doesn't have it (newer data)
    fun valueName(key: FilterKey, id: String): String = names[key]?.get(id) ?: when (key) {
        FilterKey.CreatedBy -> meName
        FilterKey.Muscle -> library.registry.muscleGroupName(id)
        FilterKey.Equipment -> library.registry.equipmentName(id)
        FilterKey.Movement -> library.registry.familyName(id)
        FilterKey.SetUp -> library.registry.positionName(id)
        FilterKey.Grip -> library.registry.gripName(id)
    }

    fun valueOrder(key: FilterKey, id: String): Int =
        filterValues[key]?.indexOfFirst { it.id == id }?.takeIf { it >= 0 } ?: Int.MAX_VALUE

    // A family's muscle group (its first primary muscle's), to order families without a muscle filter
    val familyGroups: Map<String, String> = library.registry.families.mapNotNull { family ->
        family.primaryMuscles.firstOrNull()?.let { library.registry.groupOfMuscle(it) }?.let { family.entry.id to it }
    }.toMap()

    class SearchEntry(val nameWords: List<String>, val words: List<String>)

    // Each exercise's words: its name's, its aliases', and the names and aliases of its values
    private val searchIndex: Map<String, SearchEntry> = run {
        val valueWords = filterValues.mapValues { (_, values) ->
            values.associate { value -> value.id to (searchWords(value.name) + value.aliases.flatMap { searchWords(it) }) }
        }
        exercises.associate { exercise ->
            val nameWords = searchWords(exercise.name)
            val words = nameWords +
                exercise.aliases.flatMap { searchWords(it) } +
                FilterKey.entries.flatMap { key -> values(exercise, key).flatMap { id -> valueWords[key]?.get(id).orEmpty() } }
            exercise.uuid to SearchEntry(nameWords = nameWords, words = words.distinct())
        }
    }

    fun searchEntry(exercise: LibraryExercise): SearchEntry =
        searchIndex[exercise.uuid] ?: SearchEntry(searchWords(exercise.name), searchWords(exercise.name))

    private fun com.everlog.data.model.exercise.library.ExerciseRegistry.Entry.toValue() =
        FilterValue(id = id, name = name, aliases = aliases)
}

internal fun PickerIndex.matches(exercise: LibraryExercise, token: FilterToken): Boolean =
    values(exercise, token.key).any { it in token.values } != token.not

internal fun PickerIndex.matches(exercise: LibraryExercise, tokens: List<FilterToken>, skip: FilterKey? = null): Boolean =
    tokens.all { it.key == skip || matches(exercise, it) }

// Lower case, without accents or punctuation, split into words
internal fun searchWords(text: String): List<String> =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .split(" ")
        .filter { it.isNotEmpty() }

/**
 * Whether every word of a search starts a word of the exercise's (its name, aliases, family,
 * equipment, muscle groups, set-up, grip), in any order, allowing typos: "press bench db" and
 * "benhc" both find Dumbbell Bench Press. Returns how well it matches, lower is better: the name
 * starting with the search, a name word starting with its first word, anywhere, or only with
 * typos. Null when it doesn't match.
 */
internal fun PickerIndex.searchScore(exercise: LibraryExercise, words: List<String>): Int? {
    if (words.isEmpty()) return 0
    val entry = searchEntry(exercise)
    val exact = words.all { word -> entry.words.any { it.startsWith(word) } }
    if (!exact && !words.all { word -> entry.words.any { it.startsWith(word) || fuzzyStartsWith(it, word) } }) return null
    return when {
        !exact -> 3
        entry.nameWords.joinToString(" ").startsWith(words.joinToString(" ")) -> 0
        entry.nameWords.any { it.startsWith(words.first()) } -> 1
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
