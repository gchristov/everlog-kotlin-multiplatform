package com.everlog.android.statistics.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme

// A chart's card, as on the statistics pages: its title over a line, then the chart
@Composable
fun StatisticsChartCard(title: String, modifier: Modifier = Modifier, chart: @Composable () -> Unit) {
    AppSurface(modifier = modifier.fillMaxWidth().height(StatisticsChartSizes.Height), contentPadding = PaddingValues()) {
        AppText(text = title, style = Theme.typography.bodyBold, modifier = Modifier.padding(Theme.spacing.large))
        Box(modifier = Modifier.fillMaxWidth().height(StatisticsChartSizes.Divider).background(Theme.backgrounds.separator))
        Box(modifier = Modifier.fillMaxSize()) { chart() }
    }
}

object StatisticsChartSizes {
    // The XML pages' chart cards
    val Height = 350.dp
    internal val Divider = 1.dp
}
