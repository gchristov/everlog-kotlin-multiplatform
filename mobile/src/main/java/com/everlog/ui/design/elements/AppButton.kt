package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme

// The filled green pill button, same as the XML "Button" style with the rounded_corners_btn_one background.
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = Theme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Theme.contentColors.action,
            contentColor = Theme.contentColors.onAction,
            disabledContainerColor = Theme.contentColors.action.copy(alpha = DisabledAlpha),
            disabledContentColor = Theme.contentColors.onAction,
        ),
        contentPadding = PaddingValues(
            horizontal = Theme.spacing.extraLarge,
            vertical = Theme.spacing.small,
        ),
    ) {
        AppText(
            text = text,
            color = if (enabled) Theme.contentColors.onAction else Theme.contentColors.onAction.copy(alpha = DisabledAlpha),
            style = Theme.typography.button,
            maxLines = 1,
        )
    }
}

private val ButtonMinHeight = 42.dp
private const val DisabledAlpha = 0.4f

@Preview
@Composable
private fun AppButtonPreview() {
    Theme {
        AppButton(text = "Button", onClick = {})
    }
}
