package com.everlog.ui.activities.exercisepicker

import androidx.lifecycle.SavedStateHandle
import com.everlog.data.controllers.exerciselibrary.LoadExerciseLibraryUseCase
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSetType
import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

internal enum class PickerMode {
    // Home › Exercises: a tap opens the exercise
    Browse,

    // Adding to a workout or template: a tap picks the exercise, a long press opens it
    Pick,
}

internal data class ExercisePickerState(
    val mode: PickerMode,
    val index: PickerIndex? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val tokens: List<FilterToken> = emptyList(),
    val text: String = "",
    val menu: PickerMenu = PickerMenu.Closed,
    val showAllTop: Boolean = false,
    // uuids, in the order they were picked
    val picked: List<String> = emptyList(),
    // The last filters applied (one at most), offered under Recent
    val recent: List<List<FilterToken>> = emptyList(),
    val message: PickerMessage? = null,
    // The picks to add, once Add (or a set type, for a superset) is chosen
    val result: PickerResult? = null,
    // What the screen shows, worked out from the rest off the main thread: see update()
    val content: PickerContent? = null,
)

internal sealed interface PickerMenu {
    data object Closed : PickerMenu

    // Recent searches and the keys, or suggestions while there's text
    data object Start : PickerMenu

    // One key's values, opened from a chip, the menu or a token (for analytics)
    data class Values(val key: FilterKey, val source: String) : PickerMenu
}

internal sealed interface PickerMessage {
    data class TooMany(val max: Int) : PickerMessage
}

// SINGLE adds each exercise on its own; any other type links them, like the current picker
internal data class PickerResult(val exercises: List<LibraryExercise>, val setType: ELSetType)

internal class ExercisePickerViewModel(
    dispatcher: CoroutineDispatcher,
    mode: PickerMode,
    private val savedState: SavedStateHandle,
    private val loadLibrary: LoadExerciseLibraryUseCase,
    // The user's own exercises as they change, e.g. one just created
    private val ownExerciseUpdates: () -> Flow<List<LibraryExercise>>,
    private val recentSearches: ExercisePickerRecentSearches,
    private val analytics: ExercisePickerAnalytics,
    // Created by's only value, named for the screen
    private val me: FilterValue,
    private val maxPicks: Int,
    // Where the index and the list are worked out, off the main thread
    private val contentDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CommonViewModel<ExercisePickerState>(dispatcher, ExercisePickerState(mode = mode)) {
    private var contentJob: Job? = null
    private var ownJob: Job? = null
    private var library: ExerciseLibrary? = null
    private var lastSearchLogged: String? = null
    private var lastNoResultsLogged: Pair<List<FilterToken>, String>? = null
    // A set type back from the set type picker before the library has loaded, e.g. after process death
    private var pendingAdd: ELSetType? = null

    init {
        // Back from process death: the search and the picks carry on
        setState {
            copy(
                tokens = savedState.get<ArrayList<String>>(KeyTokens).orEmpty().mapNotNull(::decodeToken),
                text = savedState[KeyText] ?: "",
                picked = savedState.get<ArrayList<String>>(KeyPicked).orEmpty(),
                showAllTop = savedState[KeyShowAllTop] ?: false,
                recent = recentSearches.load().take(1),
            )
        }
        load()
    }

    fun onRetry() = load()

    fun onSearchFocused() = update { if (menu == PickerMenu.Closed) copy(menu = PickerMenu.Start) else this }

    fun onTextChange(text: String) = update(typing = true) { copy(text = text, menu = PickerMenu.Start) }

    // Closing the menu applies the filters, so they're remembered under Recent
    fun onCloseMenu() {
        val tokens = state.value.tokens
        update { copy(menu = PickerMenu.Closed, recent = remember(tokens, recent)) }
    }

    // source: chip, menu or token
    fun onOpenKey(key: FilterKey, source: String) = update { copy(menu = PickerMenu.Values(key, source)) }

    // Analytics are logged from the state as it is, outside the reducers, so each event logs once

    fun onToggleValue(key: FilterKey, value: String) {
        val current = state.value
        val token = current.tokens.find { it.key == key }
        if (token != null && value in token.values) {
            analytics.filterRemoved(key.analyticsName)
        } else {
            analytics.filterAdded(key.analyticsName, value, (current.menu as? PickerMenu.Values)?.source ?: "menu")
        }
        update { copy(tokens = tokens.toggle(key, value)) }
    }

    // A suggestion while typing: the word being typed becomes the filter
    fun onPickSuggestion(key: FilterKey, value: String) {
        analytics.filterAdded(key.analyticsName, value, "typed")
        update {
            val words = text.trim().split(Regex("\\s+"))
            copy(tokens = tokens.add(key, value), text = words.dropLast(1).joinToString(" "))
        }
    }

    fun onToggleOperator(key: FilterKey) {
        val token = state.value.tokens.find { it.key == key } ?: return
        analytics.filterOperatorChanged(key.analyticsName, !token.not)
        update { copy(tokens = tokens.map { if (it.key == key) it.copy(not = !it.not) else it }) }
    }

    fun onRemoveToken(key: FilterKey) {
        analytics.filterRemoved(key.analyticsName)
        update { copy(tokens = tokens.filter { it.key != key }) }
    }

    fun onClearAll() {
        state.value.tokens.forEach { analytics.filterRemoved(it.key.analyticsName) }
        update { copy(tokens = emptyList(), text = "") }
    }

    fun onRecent(search: List<FilterToken>) {
        search.forEach { token -> token.values.forEach { analytics.filterAdded(token.key.analyticsName, it, "recent") } }
        update { copy(tokens = search, text = "", menu = PickerMenu.Closed) }
    }

    fun onToggleShowAll() = update { copy(showAllTop = !showAllTop) }

    // A row tapped: picked or unpicked when picking; when browsing the screen opens it
    fun onExerciseTapped(uuid: String, section: String, position: Int) {
        val current = state.value
        when {
            current.mode == PickerMode.Browse -> analytics.exercisePicked(section, position)
            uuid in current.picked -> update { copy(picked = picked - uuid) }
            current.picked.size >= maxPicks -> setState { copy(message = PickerMessage.TooMany(maxPicks)) }
            else -> {
                analytics.exercisePicked(section, position)
                update { copy(picked = picked + uuid) }
            }
        }
    }

    // source: menu (Create in the app bar's ⋮) or search (Create "…" when nothing matches)
    fun onCreateTapped(source: String) = analytics.createTapped(source)

    fun onAdd(setType: ELSetType) {
        val current = state.value
        val index = current.index ?: run {
            pendingAdd = setType
            return
        }
        val exercises = current.picked.mapNotNull { uuid -> index.exercises.find { it.uuid == uuid } }
        if (exercises.isEmpty()) return
        analytics.added(exercises.size, setType.name.lowercase(), current.tokens.size, current.text.isNotBlank())
        update { copy(result = PickerResult(exercises = exercises, setType = setType), recent = remember(current.tokens, recent)) }
    }

    // An exercise the user just created from here: picked when picking, as long as there's room.
    // It may reach the list a moment later, from Firestore.
    fun onExerciseCreated(uuid: String) {
        val current = state.value
        when {
            current.mode != PickerMode.Pick || uuid in current.picked -> Unit
            current.picked.size >= maxPicks -> setState { copy(message = PickerMessage.TooMany(maxPicks)) }
            else -> update { copy(picked = picked + uuid) }
        }
    }

    // The last filters applied, the only recent search kept
    private fun remember(tokens: List<FilterToken>, recent: List<List<FilterToken>>): List<List<FilterToken>> {
        if (tokens.isEmpty() || recent == listOf(tokens)) return recent
        val searches = listOf(tokens)
        recentSearches.save(searches)
        return searches
    }

    fun onMessageShown() = setState { copy(message = null) }

    private fun load() {
        update { copy(loading = true, failed = false) }
        launchCoroutine {
            loadLibrary().fold(
                ifLeft = {
                    Timber.tag(TAG).e(it, "Couldn't load the exercise library")
                    update { copy(loading = false, failed = true) }
                },
                ifRight = { loaded ->
                    library = loaded
                    val index = withContext(contentDispatcher) { PickerIndex(loaded, me) }
                    update { copy(index = index, loading = false, failed = false) }
                    watchOwn()
                    pendingAdd?.let {
                        pendingAdd = null
                        onAdd(it)
                    }
                },
            )
        }
    }

    // The user's own exercises follow Firestore while the picker is open, so one created or deleted
    // elsewhere shows straight away
    private fun watchOwn() {
        ownJob?.cancel()
        ownJob = launchCoroutine {
            ownExerciseUpdates().collect { own ->
                val current = library ?: return@collect
                val updated = current.copy(exercises = current.exercises.filter { !it.own } + own)
                if (updated == current) return@collect
                library = updated
                val index = withContext(contentDispatcher) { PickerIndex(updated, me) }
                // An exercise deleted elsewhere can't stay picked
                // (one just created and still on its way stays)
                val uuids = updated.exercises.map { it.uuid }.toSet()
                val before = current.exercises.map { it.uuid }.toSet()
                update { copy(index = index, picked = picked.filter { it in uuids || it !in before }) }
            }
        }
    }

    /**
     * Every change but messages goes through here. The list is worked out off the main thread, the
     * latest change cancelling one still being worked out, and while typing only once the text
     * settles: the text field itself takes each letter at once. The search and picks are kept for
     * after process death.
     */
    private fun update(typing: Boolean = false, reducer: ExercisePickerState.() -> ExercisePickerState) {
        setState(reducer)
        val state = state.value
        savedState[KeyTokens] = ArrayList(state.tokens.map(::encodeToken))
        savedState[KeyText] = state.text
        savedState[KeyPicked] = ArrayList(state.picked)
        savedState[KeyShowAllTop] = state.showAllTop
        contentJob?.cancel()
        contentJob = launchCoroutine {
            if (typing) delay(TypingDebounceMillis)
            val snapshot = this@ExercisePickerViewModel.state.value
            val index = snapshot.index ?: return@launchCoroutine
            val content = withContext(contentDispatcher) { snapshot.buildContent(index) }
            setState { copy(content = content) }
            logSearch(snapshot, content)
        }
    }

    // Once per search that settles, not per letter
    private fun logSearch(state: ExercisePickerState, content: PickerContent) {
        val text = state.text.trim()
        val words = searchWords(text)
        if (words.isNotEmpty() && text != lastSearchLogged) {
            lastSearchLogged = text
            analytics.searched(words.size, content.resultCount)
        }
        val searched = state.tokens to text
        if (content.resultCount == 0 && (state.tokens.isNotEmpty() || text.isNotEmpty()) && searched != lastNoResultsLogged) {
            lastNoResultsLogged = searched
            analytics.noResults(text.ifEmpty { null }, state.tokens.size)
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
        const val TypingDebounceMillis = 150L
        const val KeyTokens = "tokens"
        const val KeyText = "text"
        const val KeyPicked = "picked"
        const val KeyShowAllTop = "showAllTop"
    }
}

// A token as one string for the saved state: "Equipment|not|barbell,dumbbell"
internal fun encodeToken(token: FilterToken): String =
    listOf(token.key.name, if (token.not) "not" else "is", token.values.joinToString(",")).joinToString("|")

internal fun decodeToken(encoded: String): FilterToken? {
    val parts = encoded.split("|")
    if (parts.size != 3) return null
    val key = FilterKey.entries.find { it.name == parts[0] } ?: return null
    val values = parts[2].split(",").filter { it.isNotEmpty() }
    return if (values.isEmpty()) null else FilterToken(key = key, values = values, not = parts[1] == "not")
}
