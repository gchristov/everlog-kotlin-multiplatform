package com.everlog.android.common.compose.elements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme

/**
 * The shimmer for loading placeholders: a highlight sweeping across the raised card colour. Draw
 * every placeholder shape of a screen with the one brush, e.g. `Modifier.clip(shape).background(brush)`,
 * so they sweep together. Each screen lays out its own placeholders, matching what's loading.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = ShimmerMotion.From,
        targetValue = ShimmerMotion.To,
        animationSpec = infiniteRepeatable(tween(durationMillis = ShimmerMotion.Duration, easing = LinearEasing)),
        label = "sweep",
    )
    val base = Theme.backgrounds.surfaceRaised
    return Brush.linearGradient(
        colors = listOf(base, Theme.contentColors.secondary.copy(alpha = ShimmerMotion.HighlightAlpha).compositeOver(base), base),
        start = Offset(sweep * ShimmerMotion.Distance, 0f),
        end = Offset(sweep * ShimmerMotion.Distance + ShimmerMotion.Width, ShimmerMotion.Width),
    )
}

private object ShimmerMotion {
    const val Duration = 1200
    // Where the highlight starts and ends, in [Distance]s, so it enters and leaves fully off the screen
    const val From = -1f
    const val To = 2f
    const val Distance = 1000f
    const val Width = 600f
    const val HighlightAlpha = 0.2f
}

@Preview
@Composable
private fun ShimmerPreview() {
    Theme {
        val brush = rememberShimmerBrush()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        }
    }
}
