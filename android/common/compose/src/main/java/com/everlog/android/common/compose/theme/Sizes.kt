package com.everlog.android.common.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Heights that pieces sitting side by side share, so they line up.
 *
 * @param rowControl A control inside a row, e.g. a set's value box, its tick or its bin
 * @param leading What sits beside a title, e.g. an exercise's image, a drag grip or an outlined icon button
 * @param button A button, at least, and what sits beside one, e.g. the workout's options next to Finish
 */
@Immutable
data class Sizes(
    val rowControl: Dp,
    val leading: Dp,
    val button: Dp,
)

internal val LocalSizes = staticCompositionLocalOf {
    Sizes(
        rowControl = 0.dp,
        leading = 0.dp,
        button = 0.dp,
    )
}

internal fun sizes() = Sizes(
    rowControl = 36.dp,
    leading = 40.dp,
    button = 42.dp,
)
