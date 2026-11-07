package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme

@Composable
fun AppCircularProgressIndicator(modifier: Modifier = Modifier) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = Theme.contentColors.action,
    )
}

/**
 * A small spinner in a raised circle, floating over content that keeps showing beneath it, e.g. at the top of a list
 * while older items load. Raised like a snackbar, so it stands apart from the surfaces it floats over.
 */
@Composable
fun AppFloatingProgress(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Theme.backgrounds.surfaceRaised,
        shadowElevation = FloatingProgressSizes.Elevation,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.padding(FloatingProgressSizes.Padding).size(FloatingProgressSizes.Indicator),
            color = Theme.contentColors.action,
            strokeWidth = FloatingProgressSizes.Stroke,
        )
    }
}

private object FloatingProgressSizes {
    val Indicator = 24.dp
    val Padding = 8.dp
    val Stroke = 3.dp
    val Elevation = 6.dp
}
