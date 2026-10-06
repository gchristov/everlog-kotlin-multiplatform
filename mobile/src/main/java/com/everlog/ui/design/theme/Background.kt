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
    // Tint over blurred content behind bars, e.g. the footer
    val blurOverlay: Color,
    // Divider between rows in a card
    val separator: Color,
    // Faint action tint behind a selected option
    val selected: Color,
    // Stronger action tint, e.g. the current step of a progress bar
    val actionMuted: Color,
)

internal val LocalBackgrounds = staticCompositionLocalOf {
    Backgrounds(
        primary = Color.Unspecified,
        surface = Color.Unspecified,
        surfaceRaised = Color.Unspecified,
        blurOverlay = Color.Unspecified,
        separator = Color.Unspecified,
        selected = Color.Unspecified,
        actionMuted = Color.Unspecified,
    )
}

@Composable
internal fun backgrounds() = Backgrounds(
    primary = colorResource(R.color.background_base),
    surface = colorResource(R.color.background_card),
    surfaceRaised = colorResource(R.color.background_card_lighter),
    blurOverlay = colorResource(R.color.background_blur),
    separator = colorResource(R.color.separator),
    selected = colorResource(R.color.main_accent_faded_1),
    actionMuted = colorResource(R.color.main_accent_faded_2),
)
