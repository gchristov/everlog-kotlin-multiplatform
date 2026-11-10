package com.everlog.android.common.compose.components.shown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * Calls [onShown] each time this comes into view: [shown] (e.g. its tab is the current one) while the screen is in
 * the front, and [onHidden] when it leaves view again. A pager keeps its neighbouring pages composed, so being in the
 * composition doesn't mean being seen.
 */
@Composable
fun ShownEffect(shown: Boolean, onShown: () -> Unit, onHidden: () -> Unit = {}) {
    val resumed by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = shown && resumed.isAtLeast(Lifecycle.State.RESUMED)
    val latestShown by rememberUpdatedState(onShown)
    val latestHidden by rememberUpdatedState(onHidden)
    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) latestShown() else if (wasVisible) latestHidden()
        wasVisible = visible
    }
}
