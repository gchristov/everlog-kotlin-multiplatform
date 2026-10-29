package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.theme.Theme

data class AppDialogAction(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * An alert with a title, an optional line of body text and up to three buttons in a row, sized to
 * their text like the system's dialogs.
 *
 * @param primaryAction The action we want the user to take, as a filled button at the end.
 * @param secondaryAction The way out, e.g. "Skip setup", as an outlined button before it.
 * @param tertiaryAction A third, quieter choice, e.g. "Keep going" beside Discard and Finish, as a text button at the
 * start. Like the system dialog's neutral button.
 * @param content Shown under the body, e.g. a time picker.
 */
@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    primaryAction: AppDialogAction,
    modifier: Modifier = Modifier,
    body: String? = null,
    secondaryAction: AppDialogAction? = null,
    tertiaryAction: AppDialogAction? = null,
    content: (@Composable () -> Unit)? = null,
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
        text = if (body != null || content != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    body?.let {
                        AppText(
                            text = it,
                            color = Theme.contentColors.secondary,
                        )
                    }
                    content?.invoke()
                }
            }
        } else {
            null
        },
        confirmButton = {
            if (tertiaryAction != null) {
                // All three in one row, the quiet one at the start, each only as wide as its text so they fit (the
                // dialog's own row would wrap them)
                val padding = PaddingValues(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                ) {
                    AppTertiaryButton(
                        onClick = tertiaryAction.onClick,
                        text = tertiaryAction.text,
                        contentPadding = PaddingValues(horizontal = Theme.spacing.small, vertical = Theme.spacing.small),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    secondaryAction?.let { AppSecondaryButton(onClick = it.onClick, text = it.text, contentPadding = padding) }
                    AppButton(onClick = primaryAction.onClick, text = primaryAction.text, contentPadding = padding)
                }
            } else {
                AppButton(
                    onClick = primaryAction.onClick,
                    text = primaryAction.text,
                )
            }
        },
        dismissButton = secondaryAction?.takeIf { tertiaryAction == null }?.let {
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
