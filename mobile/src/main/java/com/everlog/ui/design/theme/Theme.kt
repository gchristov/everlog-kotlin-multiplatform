package com.everlog.ui.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// The app only has a dark appearance, so there are no light/dark variants yet.
@Composable
fun Theme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBackgrounds provides backgrounds(),
        LocalContentColors provides contentColors(),
        LocalTypography provides typography(),
        LocalShapes provides shapes(),
        LocalSpacing provides spacing(),
    ) {
        MaterialTheme(content = content)
    }
}

object Theme {
    val backgrounds: Backgrounds
        @Composable
        get() = LocalBackgrounds.current

    val contentColors: ContentColors
        @Composable
        get() = LocalContentColors.current

    val typography: Typography
        @Composable
        get() = LocalTypography.current

    val shapes: Shapes
        @Composable
        get() = LocalShapes.current

    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current
}
