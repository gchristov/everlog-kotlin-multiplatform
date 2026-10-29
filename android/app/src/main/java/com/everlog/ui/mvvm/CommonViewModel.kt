package com.everlog.ui.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds a screen's whole UI state in [state]. The screen collects it and calls the view-model's
 * functions for user actions.
 *
 * @param dispatcher Used in [launchCoroutine] to launch a new coroutine. The coroutine will
 * be launched on the [dispatcher] context in order to run sequential blocking operations. Most
 * common value for this should be [kotlinx.coroutines.Dispatchers.Main], and during tests
 * [kotlinx.coroutines.Dispatchers.Unconfined].
 */
abstract class CommonViewModel<S : Any>(
    private val dispatcher: CoroutineDispatcher,
    initialState: S
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)

    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * Update the state of the view-model. Collectors are only notified if the new state is different.
     */
    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    /**
     * Executes [block] in a new coroutine running in the [dispatcher] context. The calling
     * thread is not blocked and all [suspend] operations within [block] will suspend it until they
     * complete, after which execution of [block] will continue synchronously.
     */
    protected fun launchCoroutine(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(dispatcher) {
            block()
        }
}
