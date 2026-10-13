package com.everlog.ui.activities.exercisepicker

import com.everlog.config.AppConfig
import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal enum class PickerMode {
    // Home › Exercises: a tap opens the exercise
    Browse,

    // Adding to a workout or template: a tap picks the exercise, a long press opens it
    Pick,
}

internal data class ExercisePickerState(
    val mode: PickerMode = PickerMode.Pick,
    val library: PickerLibrary? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val tokens: List<FilterToken> = emptyList(),
    val text: String = "",
    val menu: PickerMenu = PickerMenu.Closed,
    val showAllTop: Boolean = false,
    // uuids, in the order they were picked
    val picked: List<String> = emptyList(),
    // Earlier searches, newest first
    val recent: List<List<FilterToken>> = emptyList(),
    val message: PickerMessage? = null,
)

internal sealed interface PickerMenu {
    data object Closed : PickerMenu

    // Recent searches and the keys, or suggestions while there's text
    data object Start : PickerMenu

    // One key's values
    data class Values(val key: FilterKey) : PickerMenu
}

internal sealed interface PickerMessage {
    data class TooMany(val max: Int) : PickerMessage

    data class Added(val count: Int, val superset: Boolean) : PickerMessage
}

internal class ExercisePickerViewModel(
    dispatcher: CoroutineDispatcher,
    mode: PickerMode,
    private val loadLibrary: suspend () -> PickerLibrary,
) : CommonViewModel<ExercisePickerState>(dispatcher, ExercisePickerState(mode = mode)) {

    init {
        load()
    }

    fun onRetry() = load()

    fun onSearchFocused() = setState { if (menu == PickerMenu.Closed) copy(menu = PickerMenu.Start) else this }

    fun onTextChange(text: String) = setState { copy(text = text, menu = PickerMenu.Start) }

    fun onCloseMenu() = setState { copy(menu = PickerMenu.Closed) }

    fun onOpenKey(key: FilterKey) = setState { copy(menu = PickerMenu.Values(key)) }

    fun onToggleValue(key: FilterKey, value: String) = setState { copy(tokens = tokens.toggle(key, value)) }

    // A suggestion while typing: the word being typed becomes the filter
    fun onPickSuggestion(key: FilterKey, value: String) = setState {
        val words = text.trim().split(Regex("\\s+"))
        copy(tokens = tokens.add(key, value), text = words.dropLast(1).joinToString(" "))
    }

    fun onToggleOperator(key: FilterKey) = setState {
        copy(tokens = tokens.map { if (it.key == key) it.copy(not = !it.not) else it })
    }

    fun onRemoveToken(key: FilterKey) = setState { copy(tokens = tokens.filter { it.key != key }) }

    fun onClearAll() = setState { copy(tokens = emptyList(), text = "") }

    fun onRecent(search: List<FilterToken>) = setState { copy(tokens = search, text = "", menu = PickerMenu.Closed) }

    fun onToggleShowAll() = setState { copy(showAllTop = !showAllTop) }

    fun onToggleExercise(uuid: String) = setState {
        val max = AppConfig.configuration.maxExerciseSelection
        when {
            uuid in picked -> copy(picked = picked - uuid)
            picked.size >= max -> copy(message = PickerMessage.TooMany(max))
            else -> copy(picked = picked + uuid)
        }
    }

    // Prototype: nothing to add them to yet, so it says what it would do and starts again
    fun onAdd(superset: Boolean) = setState {
        copy(
            message = PickerMessage.Added(count = picked.size, superset = superset),
            picked = emptyList(),
            recent = if (tokens.isEmpty()) recent else (listOf(tokens) + recent.filter { it != tokens }).take(RecentMax),
        )
    }

    fun onMessageShown() = setState { copy(message = null) }

    private fun load() {
        setState { copy(loading = true, failed = false) }
        launchCoroutine {
            val library = runCatching { loadLibrary() }
                .onFailure { Timber.tag(TAG).e(it, "Couldn't load the exercise library") }
                .getOrNull()
            setState { copy(library = library, loading = false, failed = library == null) }
        }
    }

    private fun List<FilterToken>.add(key: FilterKey, value: String): List<FilterToken> {
        val token = find { it.key == key }
        return when {
            token == null -> this + FilterToken(key = key, values = listOf(value))
            value in token.values -> this
            else -> map { if (it.key == key) it.copy(values = it.values + value) else it }
        }
    }

    private fun List<FilterToken>.toggle(key: FilterKey, value: String): List<FilterToken> {
        val token = find { it.key == key } ?: return add(key, value)
        val values = if (value in token.values) token.values - value else token.values + value
        return if (values.isEmpty()) filter { it.key != key } else map { if (it.key == key) it.copy(values = values) else it }
    }

    private companion object {
        const val TAG = "ExercisePicker"
        const val RecentMax = 3
    }
}

// What the screen shows, worked out from the state

internal data class PickerRow(
    val exercise: PickerExercise,
    val details: String,
    val picked: Boolean,
)

internal sealed interface PickerSection {
    val key: String
    val rows: List<PickerRow>

    // The exercises to reach first: most used in templates when picking, recently done when browsing
    data class Top(val kind: PickerMode, override val rows: List<PickerRow>, val total: Int, val expanded: Boolean) : PickerSection {
        override val key = "top"
    }

    data class Family(val id: String, val title: String, override val rows: List<PickerRow>) : PickerSection {
        override val key = "family_$id"
    }

    data class Own(override val rows: List<PickerRow>) : PickerSection {
        override val key = "own"
    }
}

internal data class ValueOption(val value: FilterValue, val count: Int, val picked: Boolean)

internal data class Suggestion(val key: FilterKey, val value: FilterValue, val count: Int)

internal sealed interface MenuContent {
    data class Start(val recent: List<List<FilterToken>>, val keys: List<Pair<FilterKey, String>>) : MenuContent

    data class Values(val key: FilterKey, val not: Boolean, val options: List<ValueOption>) : MenuContent

    // The keys whose name starts with [lastWord] are matched in the screen, which has their names
    data class Typing(val lastWord: String, val suggestions: List<Suggestion>, val text: String, val nameCount: Int) : MenuContent
}

internal data class PickerContent(
    val total: Int,
    val sections: List<PickerSection>,
    val resultCount: Int,
    // The keys not in the search bar yet
    val shortcuts: List<FilterKey>,
    val menu: MenuContent?,
)

private const val TopLimit = 5
private const val SuggestionLimit = 6

internal fun ExercisePickerState.content(): PickerContent? {
    val library = library ?: return null
    val words = searchWords(text)
    val scored = library.exercises.mapNotNull { exercise ->
        if (!exercise.matches(tokens)) return@mapNotNull null
        exercise.searchScore(words, library)?.let { exercise to it }
    }
    val score = scored.toMap()
    val results = scored.map { it.first }

    fun row(exercise: PickerExercise) = PickerRow(
        exercise = exercise,
        details = listOfNotNull(
            exercise.equipment?.let { library.valueName(FilterKey.Equipment, it) },
            exercise.positions.firstOrNull()?.let { library.valueName(FilterKey.SetUp, it) },
            exercise.grips.firstOrNull()?.let { library.valueName(FilterKey.Grip, it) },
        ).joinToString(" · "),
        picked = exercise.uuid in picked,
    )

    val sections = mutableListOf<PickerSection>()
    val topAll = when (mode) {
        PickerMode.Pick -> results.filter { it.templates > 0 }.sortedWith(compareBy({ -it.templates }, { -(it.lastDone ?: 0) }, { it.name }))
        PickerMode.Browse -> results.filter { it.lastDone != null }.sortedWith(compareBy({ -(it.lastDone ?: 0) }, { it.name }))
    }
    val top = if (showAllTop) topAll else topAll.take(TopLimit)
    if (top.isNotEmpty()) {
        sections += PickerSection.Top(kind = mode, rows = top.map(::row), total = topAll.size, expanded = showAllTop)
    }
    val rest = results - top.toSet()
    val byFamily = rest.filter { it.family != null }.groupBy { it.family!! }
    val muscleFiltered = tokens.any { it.key == FilterKey.Muscle && !it.not }
    val groupOrder = library.values[FilterKey.Muscle].orEmpty().map { it.id }
    val familyOrder = byFamily.keys.sortedWith(
        when {
            words.isNotEmpty() -> compareBy({ family -> byFamily.getValue(family).minOf { score.getValue(it) } }, { library.valueOrder(FilterKey.Movement, it) })
            muscleFiltered -> compareBy({ -byFamily.getValue(it).size }, { library.valueOrder(FilterKey.Movement, it) })
            else -> compareBy({ groupOrder.indexOf(library.familyGroups[it]).let { index -> if (index < 0) Int.MAX_VALUE else index } }, { library.valueOrder(FilterKey.Movement, it) })
        }
    )
    familyOrder.forEach { family ->
        val rows = byFamily.getValue(family).sortedWith(
            if (words.isNotEmpty()) compareBy({ score.getValue(it) }, { !it.familyDefault }, { it.name })
            else compareBy({ !it.familyDefault }, { it.name })
        )
        sections += PickerSection.Family(id = family, title = library.valueName(FilterKey.Movement, family), rows = rows.map(::row))
    }
    val own = rest.filter { it.family == null }.sortedBy { it.name }
    if (own.isNotEmpty()) sections += PickerSection.Own(rows = own.map(::row))

    val usedKeys = tokens.map { it.key }.toSet()
    return PickerContent(
        total = library.exercises.size,
        sections = sections,
        resultCount = results.size,
        shortcuts = FilterKey.entries.filter { it !in usedKeys && library.values[it].orEmpty().isNotEmpty() },
        menu = menuContent(library, words),
    )
}

private fun ExercisePickerState.menuContent(library: PickerLibrary, words: List<String>): MenuContent? = when (val menu = menu) {
    PickerMenu.Closed -> null
    is PickerMenu.Values -> {
        val token = tokens.find { it.key == menu.key }
        val others = library.exercises.filter { it.matches(tokens, skip = menu.key) && it.searchScore(words, library) != null }
        MenuContent.Values(
            key = menu.key,
            not = token?.not == true,
            options = library.values[menu.key].orEmpty()
                .map { value ->
                    ValueOption(
                        value = value,
                        count = others.count { value.id in it.values(menu.key) },
                        picked = token?.values?.contains(value.id) == true,
                    )
                }
                .filter { it.count > 0 || it.picked },
        )
    }

    PickerMenu.Start -> if (words.isEmpty()) {
        MenuContent.Start(
            recent = recent,
            keys = FilterKey.entries
                .filter { library.values[it].orEmpty().isNotEmpty() }
                .map { key -> key to library.values[key].orEmpty().take(4).joinToString(", ") { it.name } + "…" },
        )
    } else {
        val last = words.last()
        // Counts as if the word being typed became the filter, with the words before it still searched
        val earlier = words.dropLast(1)
        val filtered = library.exercises.filter { it.matches(tokens) && it.searchScore(earlier, library) != null }
        val suggestions = FilterKey.entries.flatMap { key ->
            val picked = tokens.find { it.key == key }?.values.orEmpty()
            library.values[key].orEmpty()
                .filter { value ->
                    val terms = searchWords(value.name) + value.aliases.flatMap { searchWords(it) }
                    value.id !in picked && terms.any { it.startsWith(last) || fuzzyStartsWith(it, last) }
                }
                .map { value -> Suggestion(key = key, value = value, count = filtered.count { value.id in it.values(key) }) }
        }
        MenuContent.Typing(
            lastWord = last,
            suggestions = suggestions.filter { it.count > 0 }.sortedByDescending { it.count }.take(SuggestionLimit),
            text = text.trim(),
            nameCount = library.exercises.count { it.matches(tokens) && it.searchScore(words, library) != null },
        )
    }
}
