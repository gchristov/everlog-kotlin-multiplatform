package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemDialogs() {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    ShowcasePage(title = "Dialogs") {
        group(key = "dialog", header = { "Title, body and two actions" }) {}
        item(key = "dialogExample") {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = true },
                text = "Show dialog",
            )
        }
    }

    if (showDialog) {
        AppDialog(
            title = "Dialog title",
            body = "A line of body text. The primary action is the one we want the user to take.",
            onDismissRequest = { showDialog = false },
            primaryAction = AppDialogAction(text = "Primary", onClick = { showDialog = false }),
            secondaryAction = AppDialogAction(text = "Secondary", onClick = { showDialog = false }),
        )
    }
}

@Preview
@Composable
private fun DesignSystemDialogsPreview() {
    Theme {
        DesignSystemDialogs()
    }
}
