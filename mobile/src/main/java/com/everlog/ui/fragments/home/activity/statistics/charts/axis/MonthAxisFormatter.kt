package com.everlog.ui.fragments.home.activity.statistics.charts.axis

import com.everlog.utils.format.FormatUtils
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import timber.log.Timber

class MonthAxisFormatter: IAxisValueFormatter {

    private val TAG = "MonthAxisFormatter"

    override fun getFormattedValue(value: Float, axis: AxisBase?): String {
        var string: String
        try {
            // Entries are displayed as DAYS so convert to MILLIS.
            string = FormatUtils.formatChartXAxisMonthString(value.toInt())
        } catch (e: Exception) {
            string = "--"
            e.printStackTrace()
            Timber.tag(TAG).w(e)
        }
        return string
    }
}
