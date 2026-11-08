package com.everlog.android.showcase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppDialog
import com.everlog.android.common.compose.elements.AppDialogAction
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppStepper
import com.everlog.android.common.compose.elements.AppTextField
import com.everlog.android.common.compose.theme.Theme

@Composable
internal fun DesignSystemDialogs() {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showThreeActions by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("Push day") }
    var field by rememberSaveable { mutableStateOf("") }
    var showStepper by rememberSaveable { mutableStateOf(false) }
    var goal by rememberSaveable { mutableStateOf(3) }

    ShowcasePage(title = "Dialogs") {
        group(key = "dialog", header = { "Title, body and two actions" }) {}
        item(key = "dialogExample") {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = true },
                text = "Show dialog",
            )
        }
        group(key = "threeActions", header = { "Three actions, e.g. leaving a workout" }) {}
        item(key = "threeActionsExample") {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showThreeActions = true },
                text = "Show dialog with three actions",
            )
        }
        group(key = "textField", header = { "Text field" }) {}
        item(key = "textFieldExample") {
            AppTextField(text = field, onTextChange = { field = it }, placeholder = "Name")
        }
        group(key = "rename", header = { "In a dialog, e.g. renaming" }) {}
        item(key = "renameExample") {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRename = true },
                text = "Rename \"$name\"",
            )
        }
        group(key = "stepper", header = { "Stepper, e.g. a weekly goal from 1 to 7" }) {}
        item(key = "stepperExample") {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showStepper = true },
                text = "Weekly goal: $goal",
            )
        }
    }

    if (showStepper) {
        var edited by rememberSaveable { mutableStateOf(goal) }
        AppDialog(
            title = "Weekly workouts goal",
            onDismissRequest = { showStepper = false },
            primaryAction = AppDialogAction(text = "Save", onClick = { goal = edited; showStepper = false }),
            secondaryAction = AppDialogAction(text = "Cancel", onClick = { showStepper = false }),
        ) {
            AppStepper(
                value = "$edited",
                onDecrease = { edited-- },
                onIncrease = { edited++ },
                decreaseDescription = "Fewer",
                increaseDescription = "More",
                canDecrease = edited > 1,
                canIncrease = edited < 7,
            )
        }
    }

    if (showRename) {
        var edited by rememberSaveable { mutableStateOf(name) }
        val save = {
            name = edited.trim().ifEmpty { name }
            showRename = false
        }
        AppDialog(
            title = "Name",
            onDismissRequest = { showRename = false },
            primaryAction = AppDialogAction(text = "Save", onClick = save),
            secondaryAction = AppDialogAction(text = "Cancel", onClick = { showRename = false }),
        ) {
            AppTextField(text = edited, onTextChange = { edited = it }, autoFocus = true, selectAllOnFocus = true, onDone = save)
        }
    }

    if (showThreeActions) {
        AppDialog(
            title = "Discard workout?",
            body = "You've completed 3 sets. Discarding deletes them.",
            onDismissRequest = { showThreeActions = false },
            primaryAction = AppDialogAction(text = "Finish", onClick = { showThreeActions = false }),
            secondaryAction = AppDialogAction(text = "Discard", onClick = { showThreeActions = false }),
            tertiaryAction = AppDialogAction(text = "Keep going", onClick = { showThreeActions = false }),
        )
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
