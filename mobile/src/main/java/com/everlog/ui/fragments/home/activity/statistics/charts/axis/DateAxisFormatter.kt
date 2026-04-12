package com.everlog.ui.fragments.home.activity.statistics.charts.axis

import com.everlog.utils.format.FormatUtils
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DateAxisFormatter: IAxisValueFormatter {

    private val TAG = "DateAxisFormatter"

    override fun getFormattedValue(value: Float, axis: AxisBase?): String {
        var string: String
        try {
            // Entries are displayed as DAYS so convert to MILLIS.
            string = FormatUtils.formatChartXAxisDayString(TimeUnit.DAYS.toMillis(value.toLong()))
        } catch (e: Exception) {
            string = "--"
            e.printStackTrace()
            Timber.tag(TAG).w(e)
        }
        return string
    }
}
