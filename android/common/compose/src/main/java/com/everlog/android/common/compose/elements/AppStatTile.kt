package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme

// One number on a card: its value, with an optional unit beside it, over a label, e.g. "2,450 kg" over "volume lifted".
// Laid out two to a row, at the screen's group spacing.
@Composable
fun AppStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    AppSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                AppText(text = value, style = Theme.typography.heading)
                unit?.let {
                    AppText(
                        text = it,
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                        modifier = Modifier.padding(start = Theme.spacing.extraSmall),
                    )
                }
            }
            AppText(text = label, style = Theme.typography.caption, color = Theme.contentColors.secondary, textAlign = TextAlign.Center)
        }
    }
}

// A stat tile while its number loads, drawn with the screen's one shimmer brush (rememberShimmerBrush)
@Composable
fun AppStatTilePlaceholder(brush: Brush, modifier: Modifier = Modifier) {
    AppSurface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().height(StatTileSizes.ContentHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small, Alignment.CenterVertically),
        ) {
            PlaceholderLine(brush, fraction = 0.35f, height = StatTileSizes.ValueHeight)
            PlaceholderLine(brush, fraction = 0.65f, height = StatTileSizes.LabelHeight)
        }
    }
}

@Composable
private fun PlaceholderLine(brush: Brush, fraction: Float, height: Dp) {
    Box(modifier = Modifier.fillMaxWidth(fraction).height(height).background(brush, RoundedCornerShape(StatTileSizes.LineCorner)))
}

private object StatTileSizes {
    // A tile's value and label, as tall as their text
    val ContentHeight = 40.dp
    val ValueHeight = 18.dp
    val LabelHeight = 10.dp
    val LineCorner = 6.dp
}

@Preview
@Composable
private fun AppStatTilePreview() {
    Theme {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                AppStatTile(value = "12", label = "workouts done", modifier = Modifier.weight(1f))
                AppStatTile(value = "2,450", unit = "kg", label = "volume lifted", modifier = Modifier.weight(1f))
            }
            val brush = rememberShimmerBrush()
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                AppStatTilePlaceholder(brush, modifier = Modifier.weight(1f))
                AppStatTilePlaceholder(brush, modifier = Modifier.weight(1f))
            }
        }
    }
}
