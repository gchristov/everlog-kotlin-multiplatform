package com.everlog.ui.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.everlog.R

@Immutable
data class ContentColors(
    val action: Color,
    // The action colour while it's pressed, e.g. a selectable card's outline
    val actionPressed: Color,
    val onAction: Color,
    val primary: Color,
    val secondary: Color,
    val destructive: Color,
)

internal val LocalContentColors = staticCompositionLocalOf {
    ContentColors(
        action = Color.Unspecified,
        actionPressed = Color.Unspecified,
        onAction = Color.Unspecified,
        primary = Color.Unspecified,
        secondary = Color.Unspecified,
        destructive = Color.Unspecified,
    )
}

@Composable
internal fun contentColors() = ContentColors(
    action = colorResource(R.color.main_accent),
    actionPressed = colorResource(R.color.main_accent_darker),
    onAction = colorResource(R.color.theme_on_accent),
    primary = colorResource(R.color.white_darker),
    secondary = colorResource(R.color.gray_1),
    destructive = colorResource(R.color.remove),
)
