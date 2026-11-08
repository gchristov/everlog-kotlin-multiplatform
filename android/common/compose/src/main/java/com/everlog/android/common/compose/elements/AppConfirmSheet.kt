package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.theme.Theme

/**
 * One of an [AppConfirmSheet]'s buttons. Its [onClick] runs once the sheet has slid away, and closes the prompt's state
 * itself (Cancel usually does what dismissing does).
 */
data class AppSheetAction(
    val text: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

/**
 * A prompt in a bottom sheet: a [title], an optional line of [body] text and any [content] under it, then its buttons,
 * full width: [primaryAction] filled (the action we want the person to take), [secondaryAction] outlined (the way out
 * or the other choice) and a quieter [tertiaryAction] as a text button, e.g. Keep going beside Finish and Discard.
 * Tapping outside or swiping it down calls [onDismissRequest].
 */
@Composable
fun AppConfirmSheet(
    title: String,
    onDismissRequest: () -> Unit,
    primaryAction: AppSheetAction,
    modifier: Modifier = Modifier,
    body: String? = null,
    secondaryAction: AppSheetAction? = null,
    tertiaryAction: AppSheetAction? = null,
    content: (@Composable () -> Unit)? = null,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, title = title) {
        body?.let { AppText(text = it, color = Theme.contentColors.secondary) }
        content?.invoke()
        AppButton(
            modifier = Modifier.fillMaxWidth().tagged(primaryAction),
            onClick = { close(primaryAction.onClick) },
            text = primaryAction.text,
        )
        secondaryAction?.let { action ->
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth().tagged(action),
                onClick = { close(action.onClick) },
                text = action.text,
            )
        }
        tertiaryAction?.let { action ->
            AppTertiaryButton(
                modifier = Modifier.fillMaxWidth().tagged(action),
                onClick = { close(action.onClick) },
                text = action.text,
            )
        }
    }
}

private fun Modifier.tagged(action: AppSheetAction) = action.testTag?.let { testTag(it) } ?: this

@Preview
@Composable
private fun AppConfirmSheetPreview() {
    Theme {
        AppConfirmSheet(
            title = "Discard workout?",
            body = "You've completed 3 sets. Discarding deletes them.",
            onDismissRequest = {},
            primaryAction = AppSheetAction("Finish", {}),
            secondaryAction = AppSheetAction("Discard", {}),
            tertiaryAction = AppSheetAction("Keep going", {}),
        )
    }
}
