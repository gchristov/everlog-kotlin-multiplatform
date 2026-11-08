package com.everlog.android.showcase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppConfirmSheet
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSheetAction
import com.everlog.android.common.compose.elements.AppTextField
import com.everlog.android.common.compose.theme.Theme

// Prompts in bottom sheets (the app has no dialogs on its Compose screens), and the text field
@Composable
internal fun DesignSystemPrompts() {
    var showPrompt by rememberSaveable { mutableStateOf(false) }
    var showThreeActions by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("Push day") }
    var field by rememberSaveable { mutableStateOf("") }

    ShowcasePage(title = "Prompts") {
        group(key = "prompt", header = { "Title, body and two actions" }) {}
        item(key = "promptExample") {
            AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = { showPrompt = true }, text = "Show prompt")
        }
        group(key = "threeActions", header = { "Three actions, e.g. leaving a workout" }) {}
        item(key = "threeActionsExample") {
            AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = { showThreeActions = true }, text = "Show prompt with three actions")
        }
        group(key = "textField", header = { "Text field" }) {}
        item(key = "textFieldExample") {
            AppTextField(text = field, onTextChange = { field = it }, placeholder = "Name")
        }
        group(key = "rename", header = { "In a prompt, above the keyboard, e.g. renaming" }) {}
        item(key = "renameExample") {
            AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = { showRename = true }, text = "Rename \"$name\"")
        }
    }

    if (showRename) {
        var edited by rememberSaveable { mutableStateOf(name) }
        val save = {
            name = edited.trim().ifEmpty { name }
            showRename = false
        }
        AppConfirmSheet(
            title = "Name",
            onDismissRequest = { showRename = false },
            primaryAction = AppSheetAction("Save", save),
            secondaryAction = AppSheetAction("Cancel", { showRename = false }),
        ) {
            AppTextField(text = edited, onTextChange = { edited = it }, autoFocus = true, selectAllOnFocus = true, onDone = save)
        }
    }

    if (showThreeActions) {
        AppConfirmSheet(
            title = "Discard workout?",
            body = "You've completed 3 sets. Discarding deletes them.",
            onDismissRequest = { showThreeActions = false },
            primaryAction = AppSheetAction("Finish", { showThreeActions = false }),
            secondaryAction = AppSheetAction("Discard", { showThreeActions = false }),
            tertiaryAction = AppSheetAction("Keep going", { showThreeActions = false }),
        )
    }

    if (showPrompt) {
        AppConfirmSheet(
            title = "Prompt title",
            body = "A line of body text. The primary action is the one we want the person to take.",
            onDismissRequest = { showPrompt = false },
            primaryAction = AppSheetAction("Primary", { showPrompt = false }),
            secondaryAction = AppSheetAction("Secondary", { showPrompt = false }),
        )
    }
}

@Preview
@Composable
private fun DesignSystemPromptsPreview() {
    Theme {
        DesignSystemPrompts()
    }
}
