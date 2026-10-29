package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSnackbarHost
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.launch

// A delete with Undo: the snackbar over the bottom of the screen until Undo or it times out
@Composable
internal fun DesignSystemSnackbar() {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sets by remember { mutableIntStateOf(4) }
    AppScreen(topBar = { AppBar(title = "Snackbar", showBack = true) }) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Box(modifier = Modifier.fillMaxSize().padding(Theme.spacing.large)) {
            AppText(text = "$sets sets")
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                enabled = sets > 0,
                onClick = {
                    sets--
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val result = snackbar.showSnackbar(message = "Set deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) sets++
                    }
                },
                text = "Delete a set",
            )
            }
            AppSnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Preview
@Composable
private fun DesignSystemSnackbarPreview() {
    Theme {
        DesignSystemSnackbar()
    }
}
