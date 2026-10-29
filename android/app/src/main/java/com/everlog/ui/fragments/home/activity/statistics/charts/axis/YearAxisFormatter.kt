package com.everlog.ui.fragments.home.activity.statistics.charts.axis

import com.everlog.utils.format.FormatUtils
import com.github.mikephil.charting.formatter.ValueFormatter
import timber.log.Timber

class YearAxisFormatter: ValueFormatter() {

    private val TAG = "YearAxisFormatter"

    override fun getFormattedValue(value: Float): String {
        var string: String
        try {
            // Entries are displayed as YEARS.
            string = FormatUtils.formatChartXAxisYearString(value.toInt())
        } catch (e: Exception) {
            string = "--"
            e.printStackTrace()
            Timber.tag(TAG).w(e)
        }
        return string
    }
}
