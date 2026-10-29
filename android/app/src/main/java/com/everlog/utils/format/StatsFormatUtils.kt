package com.everlog.utils.format

class StatsFormatUtils {

    companion object {

        fun formatNumberStatsLabel(count: Int): String {
            return if (count > 0) count.toString() + "" else "--"
        }

        fun formatWeightStatsLabel(weight: Float): String {
            return if (weight > 0) FormatUtils.formatDecimal(Math.round(weight).toFloat()) else "--"
        }

        fun formatTimeStatsLabel(timeMillis: Long): String {
            return formatTimeStatsLabel(timeMillis, "H:mm")
        }

        fun formatTimeStatsLabel(timeMillis: Long, format: String): String {
            return if (timeMillis > 0) FormatUtils.formatDurationShort(timeMillis, format, true) else "--"
        }
    }
}