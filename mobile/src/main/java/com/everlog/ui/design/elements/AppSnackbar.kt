package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme

/**
 * A short message over the bottom of the screen, with an optional action such as Undo, in the app's colours. Show
 * one with [SnackbarHostState.showSnackbar]; place the host where it should appear, e.g. aligned to the bottom of
 * the content above an [AppFooter] (the content padding's bottom).
 */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(modifier = modifier.fillMaxWidth(), hostState = hostState) { data ->
        Snackbar(
            snackbarData = data,
            // The screen's margin, less the 12dp Material's snackbar pads itself with
            modifier = Modifier.padding(horizontal = Theme.spacing.large - MaterialSnackbarPadding),
            shape = Theme.shapes.surface,
            containerColor = Theme.backgrounds.surfaceRaised,
            contentColor = Theme.contentColors.primary,
            actionColor = Theme.contentColors.action,
        )
    }
}

private val MaterialSnackbarPadding = 12.dp
