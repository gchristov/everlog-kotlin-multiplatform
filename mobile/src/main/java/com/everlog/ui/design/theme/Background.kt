package com.everlog.ui.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.everlog.R

@Immutable
data class Backgrounds(
    val primary: Color,
    val surface: Color,
    val surfaceRaised: Color,
)

internal val LocalBackgrounds = staticCompositionLocalOf {
    Backgrounds(
        primary = Color.Unspecified,
        surface = Color.Unspecified,
        surfaceRaised = Color.Unspecified,
    )
}

@Composable
internal fun backgrounds() = Backgrounds(
    primary = colorResource(R.color.background_base),
    surface = colorResource(R.color.background_card),
    surfaceRaised = colorResource(R.color.background_card_lighter),
)
