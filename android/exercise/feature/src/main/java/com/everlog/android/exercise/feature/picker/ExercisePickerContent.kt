package com.everlog.android.exercise.feature.picker

import com.everlog.android.exercise.data.model.library.LibraryExercise

// What the screen shows, worked out from the state off the main thread

internal data class PickerRow(
    val exercise: LibraryExercise,
    // What sets it apart: equipment · set-up · grip
    val details: String,
    val picked: Boolean,
    val templates: Int,
    val lastDone: Long?,
)

internal sealed interface PickerSection {
    val key: String
    val rows: List<PickerRow>
    // For analytics: which part of the list an exercise was picked from
    val analyticsName: String

    // The exercises to reach first: most used in templates when picking, recently done when browsing
    data class Top(val kind: PickerMode, override val rows: List<PickerRow>, val total: Int, val expanded: Boolean) : PickerSection {
        override val key = "top"
        override val analyticsName = if (kind == PickerMode.Pick) "most_used" else "recently_done"
    }

    data class Family(val id: String, val title: String, override val rows: List<PickerRow>) : PickerSection {
        override val key = "family_$id"
        override val analyticsName = "movement"
    }

    data class Own(override val rows: List<PickerRow>) : PickerSection {
        override val key = "own"
        override val analyticsName = "yours"
    }
}

internal data class ValueOption(val value: FilterValue, val count: Int, val picked: Boolean)

internal data class Suggestion(val key: FilterKey, val value: FilterValue, val count: Int)

internal sealed interface MenuContent {
    data class Start(val recent: List<List<FilterToken>>, val keys: List<Pair<FilterKey, String>>) : MenuContent

    data class Values(val key: FilterKey, val not: Boolean, val options: List<ValueOption>) : MenuContent

    // Keys whose name starts with [lastWord] are matched in the screen, which has their names
    data class Typing(val lastWord: String, val suggestions: List<Suggestion>, val text: String, val resultCount: Int) : MenuContent
}

internal data class PickerContent(
    val total: Int,
    val sections: List<PickerSection>,
    val resultCount: Int,
    // The keys with values that aren't in the search bar yet
    val shortcuts: List<FilterKey>,
    val menu: MenuContent?,
)

internal const val TopLimit = 5
private const val SuggestionLimit = 6
private const val HintValues = 4

internal fun ExercisePickerState.buildContent(index: PickerIndex): PickerContent {
    val words = searchWords(text)
    val scored = index.exercises.mapNotNull { exercise ->
        if (!index.matches(exercise, tokens)) return@mapNotNull null
        index.searchScore(exercise, words)?.let { exercise to it }
    }
    val score = scored.toMap()
    val results = scored.map { it.first }

    fun row(exercise: LibraryExercise) = PickerRow(
        exercise = exercise,
        details = listOfNotNull(
            exercise.equipment?.let { index.valueName(FilterKey.Equipment, it) },
            exercise.positions.firstOrNull()?.let { index.valueName(FilterKey.SetUp, it) },
            exercise.grips.firstOrNull()?.let { index.valueName(FilterKey.Grip, it) },
        ).joinToString(" · "),
        picked = exercise.uuid in picked,
        templates = index.library.templateCount(exercise),
        lastDone = index.library.lastDone(exercise),
    )

    val sections = mutableListOf<PickerSection>()
    val topAll = when (mode) {
        PickerMode.Pick -> results.filter { index.library.templateCount(it) > 0 }
            .sortedWith(compareBy({ -index.library.templateCount(it) }, { -(index.library.lastDone(it) ?: 0) }, { it.name }))
        PickerMode.Browse -> results.filter { index.library.lastDone(it) != null }
            .sortedWith(compareBy({ -(index.library.lastDone(it) ?: 0) }, { it.name }))
    }
    val top = if (showAllTop) topAll else topAll.take(TopLimit)
    if (top.isNotEmpty()) {
        sections += PickerSection.Top(kind = mode, rows = top.map(::row), total = topAll.size, expanded = showAllTop)
    }
    // Exercises in the top group aren't repeated; those past it stay in their movement
    val rest = results - top.toSet()
    val byFamily = rest.filter { it.family != null }.groupBy { it.family!! }
    val muscleFiltered = tokens.any { it.key == FilterKey.Muscle && !it.not }
    val groupOrder = index.filterValues[FilterKey.Muscle].orEmpty().map { it.id }
    val familyOrder = byFamily.keys.sortedWith(
        when {
            // While searching, the best match first
            words.isNotEmpty() -> compareBy({ family -> byFamily.getValue(family).minOf { score.getValue(it) } }, { index.valueOrder(FilterKey.Movement, it) })
            // For one muscle, the movements with the most exercises first
            muscleFiltered -> compareBy({ -byFamily.getValue(it).size }, { index.valueOrder(FilterKey.Movement, it) })
            // Otherwise in the muscle groups' order, A to Z within each
            else -> compareBy(
                { groupOrder.indexOf(index.familyGroups[it]).let { position -> if (position < 0) Int.MAX_VALUE else position } },
                { index.valueOrder(FilterKey.Movement, it) },
            )
        }
    )
    familyOrder.forEach { family ->
        val rows = byFamily.getValue(family).sortedWith(
            if (words.isNotEmpty()) compareBy({ score.getValue(it) }, { !it.familyDefault }, { it.name })
            else compareBy({ !it.familyDefault }, { it.name })
        )
        sections += PickerSection.Family(id = family, title = index.valueName(FilterKey.Movement, family), rows = rows.map(::row))
    }
    // Exercises without a family: the user's own, and library ones whose family is newer than the app
    val unfamilied = rest.filter { it.family == null }.sortedBy { it.name }
    if (unfamilied.isNotEmpty()) sections += PickerSection.Own(rows = unfamilied.map(::row))

    val usedKeys = tokens.map { it.key }.toSet()
    return PickerContent(
        total = index.exercises.size,
        sections = sections,
        resultCount = results.size,
        shortcuts = FilterKey.entries.filter { it !in usedKeys && index.filterValues[it].orEmpty().isNotEmpty() },
        menu = menuContent(index, words),
    )
}

private fun ExercisePickerState.menuContent(index: PickerIndex, words: List<String>): MenuContent? = when (val menu = menu) {
    PickerMenu.Closed -> null
    is PickerMenu.Values -> {
        val token = tokens.find { it.key == menu.key }
        val others = index.exercises.filter { index.matches(it, tokens, skip = menu.key) && index.searchScore(it, words) != null }
        MenuContent.Values(
            key = menu.key,
            not = token?.not == true,
            options = index.filterValues[menu.key].orEmpty()
                .map { value ->
                    ValueOption(
                        value = value,
                        count = others.count { value.id in index.values(it, menu.key) },
                        picked = token?.values?.contains(value.id) == true,
                    )
                }
                // Values with nothing left to show go, unless already picked
                .filter { it.count > 0 || it.picked },
        )
    }

    is PickerMenu.Start -> if (words.isEmpty()) {
        MenuContent.Start(
            recent = recent,
            keys = FilterKey.entries
                .filter { index.filterValues[it].orEmpty().isNotEmpty() }
                .map { key ->
                    val values = index.filterValues[key].orEmpty()
                    key to values.take(HintValues).joinToString(", ") { it.name } + if (values.size > HintValues) "…" else ""
                },
        )
    } else {
        val last = words.last()
        // Counted as if the word being typed became the filter, with the words before it still searched
        val earlier = words.dropLast(1)
        val filtered = index.exercises.filter { index.matches(it, tokens) && index.searchScore(it, earlier) != null }
        val suggestions = FilterKey.entries.flatMap { key ->
            val picked = tokens.find { it.key == key }?.values.orEmpty()
            index.filterValues[key].orEmpty()
                .filter { value ->
                    val terms = searchWords(value.name) + value.aliases.flatMap { searchWords(it) }
                    value.id !in picked && terms.any { it.startsWith(last) || fuzzyStartsWith(it, last) }
                }
                .map { value -> Suggestion(key = key, value = value, count = filtered.count { value.id in index.values(it, key) }) }
        }
        MenuContent.Typing(
            lastWord = last,
            suggestions = suggestions.filter { it.count > 0 }.sortedByDescending { it.count }.take(SuggestionLimit),
            text = text.trim(),
            resultCount = index.exercises.count { index.matches(it, tokens) && index.searchScore(it, words) != null },
        )
    }
}
