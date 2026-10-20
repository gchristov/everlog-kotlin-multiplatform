package com.everlog.ui.design.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    contentPadding: PaddingValues = buttonContentPadding(),
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
        contentPadding = contentPadding,
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
    contentPadding: PaddingValues = buttonContentPadding(),
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
        contentPadding = contentPadding,
    ) {
        ButtonText(
            text = text,
            color = color,
            enabled = enabled,
        )
    }
}

// Green text with no outline or fill, same as the XML "Button.Four" style. In an app bar, pass a
// smaller contentPadding (e.g. 12dp) so the text lines up with the screen's margin.
@Composable
fun AppTertiaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
    contentPadding: PaddingValues = buttonContentPadding(),
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = Theme.shapes.button,
        contentPadding = contentPadding,
    ) {
        ButtonText(
            text = text,
            color = Theme.contentColors.action,
            enabled = enabled,
        )
    }
}

/**
 * @param outlined In a thin circle, for an icon button that stands on its own next to content rather than in an
 * app bar, e.g. a card's ⋮ menu.
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color = Theme.contentColors.primary,
    contentDescription: String? = null,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = if (outlined) modifier.outlinedCircle() else modifier,
        enabled = enabled,
    ) {
        AppIcon(
            imageVector = icon,
            tint = if (enabled) tint else tint.copy(alpha = DisabledAlpha),
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
            AppTertiaryButton(onClick = {}, text = "Tertiary")
        }
    }
}

// The outlined icon button's circle: the icon button's own size, its touch area still the minimum 48dp
@Composable
private fun Modifier.outlinedCircle(): Modifier = this
    .size(OutlinedIconButtonSize)
    .border(1.dp, Theme.backgrounds.separator, CircleShape)

private val OutlinedIconButtonSize = 40.dp
