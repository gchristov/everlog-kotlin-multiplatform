package com.everlog.android.statistics.ui.page

import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.common.kotlin.makeSingleLine
import com.everlog.android.common.mvp.chart.ChartRenderer
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.ui.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import org.threeten.bp.LocalDate

// The three charts, MPAndroidChart's views drawn by ChartRenderer as on the XML page, inside Compose cards. Without
// [touchable], e.g. in an example, they don't answer touches, which reach them past Compose.
@Composable
internal fun StatisticsCharts(state: StatisticsPageState, modifier: Modifier = Modifier, touchable: Boolean = true) {
    val context = LocalContext.current
    val renderer = remember(context) { ChartRenderer(context) }
    val axis = remember(state.period, state.today) { AxisLabels(state.period, state.today) }
    val stats = state.stats
    Column(modifier = modifier) {
        ChartCard(title = stringResource(R.string.statistics_muscles_trained), modifier = Modifier.testTag(StatisticsPageTags.MusclesChart)) {
            AndroidView(
                factory = { PieChart(it) },
                update = { chart ->
                    renderer.renderPieChart(chart, stats.muscles.toSortedMap(String.CASE_INSENSITIVE_ORDER).map { (category, count) -> PieEntry(count.toFloat(), categoryTitle(category)) })
                    chart.setTouchEnabled(touchable)
                },
                modifier = Modifier.fillMaxSize().padding(horizontal = ChartSizes.SideMargin, vertical = Theme.spacing.large),
            )
        }
        val weightTitle = "${stringResource(DesignR.string.weight)} (${weightUnitAbbreviation(state.weightUnit)})"
        ChartCard(title = stringResource(DesignR.string.home_week_weight_lifted).makeSingleLine(), modifier = Modifier.padding(top = Theme.spacing.large)) {
            AndroidView(
                factory = { LayoutInflater.from(it).inflate(R.layout.view_statistics_bar_chart, null) },
                update = { view ->
                    view.findViewById<TextView>(R.id.axisTitleLbl).text = weightTitle
                    val chart = view.findViewById<BarChart>(R.id.chart)
                    renderer.renderBarChart(
                        chart = chart,
                        axisLeftLabelY = view.findViewById(R.id.axisTitle),
                        granularityAxisX = 1f,
                        formatterAxisX = axis,
                        data = arrayOf(ChartRenderer.ChartDataDescriptor(stats.weightLifted.map { BarEntry(state.period.chartX(it.start), it.value) })),
                    )
                    chart.setTouchEnabled(touchable)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        val minutes = stringResource(R.string.statistics_minutes)
        val durationTitle = stringResource(if (state.period == StatsPeriod.Last12Months) R.string.statistics_session_duration_avg else R.string.statistics_session_duration)
        ChartCard(title = durationTitle, modifier = Modifier.padding(top = Theme.spacing.large)) {
            AndroidView(
                factory = { LayoutInflater.from(it).inflate(R.layout.view_statistics_line_chart, null) },
                update = { view ->
                    view.findViewById<TextView>(R.id.axisTitleLbl).text = minutes
                    val chart = view.findViewById<LineChart>(R.id.chart)
                    renderer.renderLineChart(
                        chart = chart,
                        axisLeftLabelY = view.findViewById(R.id.axisTitle),
                        granularityAxisX = 1f,
                        formatterAxisX = axis,
                        data = arrayOf(ChartRenderer.ChartDataDescriptor(stats.duration.map { Entry(state.period.chartX(it.start), it.value) })),
                    )
                    chart.setTouchEnabled(touchable)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// A chart's card: its title over a line, then the chart at the XML page's height
@Composable
private fun ChartCard(title: String, modifier: Modifier = Modifier, chart: @Composable () -> Unit) {
    AppSurface(modifier = modifier.fillMaxWidth().height(ChartSizes.Height), contentPadding = PaddingValues()) {
        AppText(text = title, style = Theme.typography.bodyBold, modifier = Modifier.padding(Theme.spacing.large))
        Box(modifier = Modifier.fillMaxWidth().height(ChartSizes.Divider).background(Theme.backgrounds.separator))
        Box(modifier = Modifier.fillMaxSize()) { chart() }
    }
}

private class AxisLabels(private val period: StatsPeriod, private val today: LocalDate) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = period.chartLabel(value, today)
}

private fun categoryTitle(category: String): String =
    ArrayResourceTypeUtils.withExerciseCategories().getTitle(category, category.lowercase().replaceFirstChar { it.uppercase() }).orEmpty()

private object ChartSizes {
    val Height = 350.dp
    val Divider = 1.dp
    val SideMargin = 10.dp
}
