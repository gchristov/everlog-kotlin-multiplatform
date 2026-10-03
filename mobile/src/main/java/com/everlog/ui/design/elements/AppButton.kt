package com.everlog.ui.design.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme

// The filled green pill button, same as the XML "Button" style with the rounded_corners_btn_one background.
@Composable
fun AppButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = Theme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Theme.contentColors.action,
            disabledContainerColor = Theme.contentColors.action.copy(alpha = DisabledAlpha),
        ),
        contentPadding = buttonContentPadding(),
    ) {
        ButtonText(
            text = text,
            color = Theme.contentColors.onAction,
            enabled = enabled,
        )
    }
}

// The outlined green pill button, same as the XML "Button.Three" style.
@Composable
fun AppSecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
) {
    val color = Theme.contentColors.action
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = Theme.shapes.button,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        ),
        contentPadding = buttonContentPadding(),
    ) {
        ButtonText(
            text = text,
            color = color,
            enabled = enabled,
        )
    }
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color = Theme.contentColors.primary,
    contentDescription: String? = null,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        AppIcon(
            imageVector = icon,
            tint = tint,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun ButtonText(
    text: String,
    color: Color,
    enabled: Boolean,
) {
    AppText(
        text = text,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        style = Theme.typography.button,
        maxLines = 1,
    )
}

@Composable
private fun buttonContentPadding() = PaddingValues(
    horizontal = Theme.spacing.extraLarge,
    vertical = Theme.spacing.small,
)

private val ButtonMinHeight = 42.dp
private const val DisabledAlpha = 0.4f

@Preview
@Composable
private fun AppButtonPreview() {
    Theme {
        Column {
            AppButton(onClick = {}, text = "Primary")
            AppSecondaryButton(onClick = {}, text = "Secondary")
        }
    }
}
