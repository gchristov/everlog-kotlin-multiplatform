package com.everlog.android.statistics.ui.chart

import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.chart.ChartRenderer
import com.everlog.android.statistics.ui.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter

// A point on a chart: [x] along the bottom, e.g. a day or month counted from 1970, and its value
data class ChartPoint(val x: Float, val y: Float)

/**
 * A line of a [StatisticsLineChart], named in the chart's legend when there's more than one.
 *
 * @param rightAxis Measured on the right axis, e.g. weight next to reps on the left
 */
data class ChartLine(
    val points: List<ChartPoint>,
    val name: String? = null,
    @param:ColorRes val color: Int = DesignR.color.pie_chart_8,
    val rightAxis: Boolean = false,
)

/**
 * MPAndroidChart's line chart, drawn by ChartRenderer as on the XML pages, with what each axis measures up its side.
 * Without [touchable], e.g. in an example, it doesn't answer touches, which reach it past Compose.
 *
 * @param xLabel The label under a point's x
 */
@Composable
fun StatisticsLineChart(
    lines: List<ChartLine>,
    leftAxisTitle: String,
    xLabel: (Float) -> String,
    modifier: Modifier = Modifier,
    rightAxisTitle: String? = null,
    touchable: Boolean = true,
) {
    val context = LocalContext.current
    val renderer = remember(context) { ChartRenderer(context) }
    AndroidView(
        factory = { LayoutInflater.from(it).inflate(R.layout.view_statistics_line_chart, null) },
        update = { view ->
            view.findViewById<TextView>(R.id.axisTitleLbl).text = leftAxisTitle
            view.findViewById<TextView>(R.id.axisRightTitleLbl).text = rightAxisTitle
            val chart = view.findViewById<LineChart>(R.id.chart)
            renderer.renderLineChart(
                chart = chart,
                axisLeftLabelY = view.findViewById(R.id.axisTitle),
                axisRightLabelY = view.findViewById(R.id.axisRightTitle),
                granularityAxisX = 1f,
                formatterAxisX = XLabels(xLabel),
                data = lines.map { line ->
                    ChartRenderer.ChartDataDescriptor(
                        entries = line.points.map { Entry(it.x, it.y) },
                        colorResId = line.color,
                        axisDependency = if (line.rightAxis) YAxis.AxisDependency.RIGHT else YAxis.AxisDependency.LEFT,
                        title = line.name,
                    )
                }.toTypedArray(),
            )
            chart.setTouchEnabled(touchable)
        },
        modifier = modifier.fillMaxSize(),
    )
}

private class XLabels(private val label: (Float) -> String) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = label(value)
}
