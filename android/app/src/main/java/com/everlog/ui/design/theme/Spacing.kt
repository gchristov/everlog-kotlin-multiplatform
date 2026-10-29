package com.everlog.ui.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Follows the activity_margin* dimens.
@Immutable
data class Spacing(
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
)

internal val LocalSpacing = staticCompositionLocalOf {
    Spacing(
        extraSmall = 0.dp,
        small = 0.dp,
        medium = 0.dp,
        large = 0.dp,
        extraLarge = 0.dp,
    )
}

internal fun spacing() = Spacing(
    extraSmall = 4.dp,
    small = 8.dp,
    medium = 12.dp,
    large = 16.dp,
    extraLarge = 32.dp,
)
