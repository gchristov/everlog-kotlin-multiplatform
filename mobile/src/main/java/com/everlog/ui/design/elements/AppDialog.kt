package com.everlog.ui.design.elements

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.theme.Theme

data class AppDialogAction(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * An alert with a title, an optional line of body text and up to two buttons side by side, sized
 * to their text like the system's dialogs.
 *
 * @param primaryAction The action we want the user to take, as a filled button.
 * @param secondaryAction The way out, e.g. "Skip setup", as an outlined button before it.
 */
@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    primaryAction: AppDialogAction,
    modifier: Modifier = Modifier,
    body: String? = null,
    secondaryAction: AppDialogAction? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        containerColor = Theme.backgrounds.surface,
        title = {
            AppText(
                text = title,
                style = Theme.typography.heading,
            )
        },
        text = body?.let {
            {
                AppText(
                    text = it,
                    color = Theme.contentColors.secondary,
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = primaryAction.onClick,
                text = primaryAction.text,
            )
        },
        dismissButton = secondaryAction?.let {
            {
                AppSecondaryButton(
                    onClick = it.onClick,
                    text = it.text,
                )
            }
        },
    )
}

@Preview
@Composable
private fun AppDialogPreview() {
    Theme {
        AppDialog(
            title = "Skip setup?",
            body = "We'll start you with an empty app.",
            onDismissRequest = {},
            primaryAction = AppDialogAction(text = "Keep going", onClick = {}),
            secondaryAction = AppDialogAction(text = "Skip setup", onClick = {}),
        )
    }
}
