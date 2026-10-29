package com.everlog.ui.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class Shapes(
    val surface: Shape,
    val button: Shape,
    // Rows in a grouped list, which together look like one card
    val groupStart: Shape,
    val groupMiddle: Shape,
    val groupEnd: Shape,
    val groupSingle: Shape,
)

internal val LocalShapes = staticCompositionLocalOf {
    Shapes(
        surface = RoundedCornerShape(ZeroCornerSize),
        button = RoundedCornerShape(ZeroCornerSize),
        groupStart = RoundedCornerShape(ZeroCornerSize),
        groupMiddle = RoundedCornerShape(ZeroCornerSize),
        groupEnd = RoundedCornerShape(ZeroCornerSize),
        groupSingle = RoundedCornerShape(ZeroCornerSize),
    )
}

internal fun shapes() = Shapes(
    surface = RoundedCornerShape(size = CornerRadius),
    // Pill, like rounded_corners_btn_one
    button = RoundedCornerShape(percent = 50),
    groupStart = RoundedCornerShape(
        topStart = CornerRadius,
        topEnd = CornerRadius
    ),
    groupMiddle = RectangleShape,
    groupEnd = RoundedCornerShape(
        bottomStart = CornerRadius,
        bottomEnd = CornerRadius
    ),
    groupSingle = RoundedCornerShape(size = CornerRadius),
)

// card_radius_large
private val CornerRadius = 24.dp
