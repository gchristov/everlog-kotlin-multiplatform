package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppCircularProgressIndicator
import com.everlog.ui.design.elements.rememberShimmerBrush
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemProgress() {
    ShowcasePage(title = "Progress") {
        group(key = "indicator", header = { "Circular progress indicator" }) {}
        item(key = "indicatorExample") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        }
        group(key = "shimmer", header = { "Shimmer" }) {}
        item(key = "shimmerExample") {
            val brush = rememberShimmerBrush()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.surface)
                    .background(Theme.backgrounds.surface),
            ) {
                repeat(3) { index ->
                    Row(
                        modifier = Modifier.padding(Theme.spacing.large),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(brush))
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                            Box(modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.55f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                            Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemProgressPreview() {
    Theme {
        DesignSystemProgress()
    }
}
