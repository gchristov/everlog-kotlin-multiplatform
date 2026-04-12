package com.everlog.ui.fragments.home.activity.statistics.charts

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import com.everlog.R
import com.everlog.ui.fragments.home.activity.statistics.charts.axis.CustomXAxisRenderer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF

class ChartRenderer(context: Context) {

    private var mChartColors = ArrayList<Int>()

    init {
        setupChartColors(context)
    }

    // Bar chart

    fun renderBarChart(chart: BarChart,
                       axisLeftLabelY: View? = null,
                       axisRightLabelY: View? = null,
                       granularityAxisX: Float,
                       formatterAxisX: IAxisValueFormatter,
                       data: Array<ChartDataDescriptor<BarEntry>>) {
        setupBarChart(chart)
        chart.clear()
        chart.data = null
        chart.legend?.isEnabled = data.size > 1
        chart.axisRight?.isEnabled = false
        axisLeftLabelY?.visibility = View.GONE
        axisRightLabelY?.visibility = View.GONE
        // Build the data sets
        val dataSets = ArrayList<IBarDataSet>()
        data
                .filter { it.entries.isNotEmpty() }
                .forEachIndexed { index, chartData ->
                    val set = BarDataSet(chartData.entries, chartData.title ?: "DataSet " + (index + 1))
                    set.color = ContextCompat.getColor(chart.context, chartData.colorResId)
                    set.axisDependency = chartData.axisDependency
                    if (!chart.axisRight.isEnabled) {
                        chart.axisRight?.isEnabled = set.axisDependency == YAxis.AxisDependency.RIGHT
                        axisRightLabelY?.visibility = if (chart.axisRight?.isEnabled == true) View.VISIBLE else View.GONE
                    }
                    set.setDrawIcons(false)
                    set.setDrawValues(false)
                    dataSets.add(set)
        }
        if (dataSets.isNotEmpty()) {
            // Set granularity and formatter
            val xAxis = chart.xAxis
            xAxis?.granularity = granularityAxisX
//            xAxis?.valueFormatter = formatterAxisX
            // Set data set
            val barData = BarData(dataSets)
            barData.isHighlightEnabled = false
            chart.data = barData
        }
        // Refresh chart
        axisLeftLabelY?.visibility = if (dataSets.isEmpty()) View.GONE else View.VISIBLE
        chart.fitScreen()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    // Line chart

    fun renderLineChart(chart: LineChart,
                        axisLeftLabelY: View? = null,
                        axisRightLabelY: View? = null,
                        granularityAxisX: Float,
                        formatterAxisX: IAxisValueFormatter,
                        data: Array<ChartDataDescriptor<Entry>>) {
        setupBarChart(chart)
        chart.clear()
        chart.data = null
        chart.legend?.isEnabled = data.size > 1
        chart.axisRight?.isEnabled = false
        axisLeftLabelY?.visibility = View.GONE
        axisRightLabelY?.visibility = View.GONE
        // Build the data sets
        val dataSets = ArrayList<ILineDataSet>()
        data
                .filter { it.entries.isNotEmpty() }
                .forEachIndexed { index, chartData ->
                    val cols = ArrayList<Int>()
                    cols.add(ContextCompat.getColor(chart.context, chartData.colorResId))
                    val set = LineDataSet(chartData.entries, chartData.title ?: "DataSet " + (index + 1))
                    set.lineWidth = 1f
                    set.axisDependency = chartData.axisDependency
                    if (!chart.axisRight.isEnabled) {
                        chart.axisRight?.isEnabled = set.axisDependency == YAxis.AxisDependency.RIGHT
                        axisRightLabelY?.visibility = if (chart.axisRight?.isEnabled == true) View.VISIBLE else View.GONE
                    }
                    set.color = ContextCompat.getColor(chart.context, chartData.colorResId)
                    set.setDrawIcons(false)
                    set.setDrawCircles(true)
                    set.circleRadius = 2F
                    set.circleColors = cols
                    set.setDrawCircleHole(false)
                    set.setDrawValues(false)
                    set.mode = LineDataSet.Mode.CUBIC_BEZIER // Bezier curves
//                    set.setDrawFilled(data.size == 1)
//                    if (set.isDrawFilledEnabled) {
//                        if (Utils.getSDKInt() >= 18) {
//                            // Fill drawable only supported on api level 18 and above
//                            val drawable = ContextCompat.getDrawable(chart.context, R.drawable.chart_gradient)
//                            set.fillDrawable = drawable
//                        } else {
//                            set.fillColor = ContextCompat.getColor(chart.context, R.color.main_accent)
//                        }
//                    }
                    dataSets.add(set)
        }
        if (dataSets.isNotEmpty()) {
            // Set granularity and formatter
            val xAxis = chart.xAxis
            xAxis?.granularity = granularityAxisX
//            xAxis?.valueFormatter = formatterAxisX
            // Set data set
            val lineData = LineData(dataSets)
            lineData.isHighlightEnabled = false
            chart.data = lineData
        }
        // Refresh chart
        axisLeftLabelY?.visibility = if (dataSets.isEmpty()) View.GONE else View.VISIBLE
        chart.fitScreen()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    // Pie chart

    fun renderPieChart(chart: PieChart, data: List<PieEntry>) {
        setupPieChart(chart)
        chart.clear()
        chart.data = null
        if (data.isNotEmpty()) {
            // Build the data set
            val dataSet = PieDataSet(data, "")
            dataSet.setDrawIcons(false)
            dataSet.sliceSpace = 3f
            dataSet.iconsOffset = MPPointF(0f, 40f)
            dataSet.selectionShift = 5f
            dataSet.colors = mChartColors
            // Add data set to chart
            val pieData = PieData(dataSet)
            pieData.setValueFormatter(PercentFormatter())
            pieData.setValueTextSize(12f)
            pieData.setValueTextColor(ContextCompat.getColor(chart.context, R.color.white_base))
            pieData.setValueTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            chart.data = pieData
        }
        // Refresh chart
        chart.highlightValues(null)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun getPieCenterText(context: Context): SpannableString {
        val txt = context.getString(R.string.statistics_muscles_trained_tap_prompt)
        val s = SpannableString(txt)
        s.setSpan(RelativeSizeSpan(1f), 0, s.length, 0)
        s.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray_1)), 0, s.length, 0)
        return s
    }

    private fun getPieCenterSelectedText(context: Context,
                                         category: String,
                                         exercises: Int): SpannableString {
        val txt = context.resources.getQuantityString(R.plurals.statistics_category_exercises, exercises, category, exercises)
        val catIndex = txt.indexOf(category)
        val exIndex = txt.indexOf(exercises.toString())
        val s = SpannableString(txt)
        s.setSpan(RelativeSizeSpan(1.5f), catIndex, category.length, 0)
        s.setSpan(StyleSpan(Typeface.BOLD), catIndex, category.length, 0)
        s.setSpan(ForegroundColorSpan(Color.WHITE), catIndex, category.length, 0)
        s.setSpan(RelativeSizeSpan(1f), exIndex, s.length, 0)
        s.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray_1)), exIndex, s.length, 0)
        return s
    }

    // Setup

    private fun setupChartColors(context: Context) {
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_8))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_7))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_6))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_5))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_4))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_3))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_2))
        mChartColors.add(ContextCompat.getColor(context, R.color.pie_chart_1))
    }

    private fun setupBarChart(chart: BarLineChartBase<*>) {
        chart.setNoDataTextColor(ContextCompat.getColor(chart.context, R.color.gray_1))
        chart.description?.isEnabled = false
        chart.isDragEnabled = true
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setTouchEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setScaleEnabled(true)
        chart.viewPortHandler?.setMaximumScaleX(6f)
        chart.viewPortHandler?.setMaximumScaleY(6f)
        chart.extraBottomOffset = 14F
        // X axis
        val xAxis = chart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.setDrawGridLines(false)
        xAxis?.textColor = ContextCompat.getColor(chart.context, R.color.gray_2)
        xAxis?.textSize = 12f
        xAxis?.yOffset = 14F
        chart.axisLeft?.setDrawGridLines(false)
        chart.axisLeft?.textColor = xAxis?.textColor ?: -1
        chart.axisLeft?.textSize = 12f
        chart.axisLeft?.axisMinimum = 0F
        chart.axisRight?.setDrawGridLines(false)
        chart.axisRight?.textColor = xAxis?.textColor ?: -1
        chart.axisRight?.textSize = 12f
        chart.axisRight?.axisMinimum = 0F
        // Set renderer to have multiple lines on labels
        chart.setXAxisRenderer(CustomXAxisRenderer(chart.viewPortHandler, chart.xAxis, chart.getTransformer(YAxis.AxisDependency.LEFT)))
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        chart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        chart.legend.setDrawInside(false)
        chart.legend.textColor = ContextCompat.getColor(chart.context, R.color.gray_2)
        chart.legend.textSize = 12f
        chart.legend.xEntrySpace = 10f
        chart.legend.yEntrySpace = 0f
        chart.legend.yOffset = 5f
    }

    private fun setupPieChart(chart: PieChart) {
        chart.setNoDataTextColor(ContextCompat.getColor(chart.context, R.color.gray_1))
        chart.setUsePercentValues(true)
        chart.description?.isEnabled = false
        chart.dragDecelerationFrictionCoef = 0.95f
        chart.setHoleColor(Color.TRANSPARENT)
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 56f
        chart.transparentCircleRadius = 59f
        chart.setTransparentCircleAlpha(110)
        chart.setTransparentCircleColor(ContextCompat.getColor(chart.context, R.color.white_base))
        chart.setDrawCenterText(true)
        chart.centerText = getPieCenterText(chart.context)
        chart.rotationAngle = 0F
        chart.isRotationEnabled = true
        chart.isHighlightPerTapEnabled = true
        chart.setDrawEntryLabels(false)
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onNothingSelected() {
                chart.centerText = getPieCenterText(chart.context)
                chart.invalidate()
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                chart.centerText = getPieCenterSelectedText(chart.context, (e as? PieEntry)!!.label, e.y.toInt())
                chart.invalidate()
            }
        })
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        chart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        chart.legend.setDrawInside(false)
        chart.legend.textColor = ContextCompat.getColor(chart.context, R.color.gray_2)
        chart.legend.textSize = 12f
        chart.legend.xEntrySpace = 10f
        chart.legend.yEntrySpace = 0f
        chart.legend.yOffset = 5f
    }

    data class ChartDataDescriptor<T>(

            var entries: List<T> = ArrayList(),
            var colorResId: Int = R.color.pie_chart_8,
            var axisDependency: YAxis.AxisDependency = YAxis.AxisDependency.LEFT,
            var title: String? = null
    )
}