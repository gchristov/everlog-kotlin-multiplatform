package com.everlog.android.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppStatTile
import com.everlog.android.common.compose.elements.AppStatTilePlaceholder
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme

@Composable
internal fun DesignSystemStatTiles() {
    ShowcasePage(title = "Stat tiles") {
        group(key = "tiles", header = { "Two to a row" }) {}
        item(key = "tilesExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    AppStatTile(value = "12", label = "workouts done", modifier = Modifier.weight(1f))
                    AppStatTile(value = "24,580", unit = "kg", label = "volume lifted", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    AppStatTile(value = "1:05", unit = "h", label = "avg. session duration", modifier = Modifier.weight(1f))
                    AppStatTile(value = "140", unit = "kg", label = "max weight", modifier = Modifier.weight(1f))
                }
            }
        }
        group(key = "loading", header = { "Loading" }) {}
        item(key = "loadingExample") {
            val brush = rememberShimmerBrush()
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                AppStatTilePlaceholder(brush, modifier = Modifier.weight(1f))
                AppStatTilePlaceholder(brush, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemStatTilesPreview() {
    Theme {
        DesignSystemStatTiles()
    }
}
