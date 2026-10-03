package com.everlog.ui.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class Shapes(
    val surface: Shape,
    val button: Shape,
)

internal val LocalShapes = staticCompositionLocalOf {
    Shapes(
        surface = RoundedCornerShape(ZeroCornerSize),
        button = RoundedCornerShape(ZeroCornerSize),
    )
}

internal fun shapes() = Shapes(
    // card_radius_default
    surface = RoundedCornerShape(size = 10.dp),
    // Pill, like rounded_corners_btn_one
    button = RoundedCornerShape(percent = 50),
)
