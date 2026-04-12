package com.everlog.utils.format

import com.everlog.utils.NumberUtils
import com.everlog.utils.isSameYear
import org.apache.commons.lang3.time.DurationFormatUtils
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.util.*

class FormatUtils {

    companion object {

        private val format = DecimalFormat("0.##")

        @JvmStatic
        fun formatSetWeight(weight: Float): String {
            // Not using FormatUtils because it will round up any decimal values.
            return if (NumberUtils.isWhole(weight)) {
                weight.toInt().toString() + ""
            } else formatDecimal(weight)
        }

        fun formatDecimal(number: Float): String {
            return format.format(number.toDouble())
        }

        @JvmStatic
        fun formatSetTime(timeSeconds: Int): String {
            return formatDurationFull(timeSeconds)
        }

        @JvmStatic
        fun formatRestTime(timeSeconds: Int): String {
            var formatted = formatDurationFull(timeSeconds)
            if (formatted.isEmpty()) {
                formatted = "No rest"
            }
            return formatted
        }

        @JvmStatic
        fun formatExerciseTime(timeSeconds: Int, mixed: Boolean): String {
            if (mixed) {
                return "Mixed"
            } else if (timeSeconds <= 0) {
                return "--"
            }
            return formatSetTime(timeSeconds)
        }

        private fun formatDurationFull(timeSeconds: Int): String {
            // Calculate time
            val m = timeSeconds / 60
            val s = timeSeconds % 60
            var restTimeValue = ""
            if (timeSeconds <= 0) {
                return restTimeValue
            }
            if (m != 0) {
                restTimeValue += String.format("%d min", m)
            }
            if (s != 0) {
                restTimeValue += (if (restTimeValue.isNotEmpty()) " " else "") + String.format("%d sec", s)
            }
            return restTimeValue
        }

        @JvmStatic
        fun formatDurationShort(durationMillis: Long, format: String): String {
            return formatDurationShort(durationMillis, format, true)
        }

        fun formatDurationShort(durationMillis: Long, format: String, padWithZeros: Boolean): String {
            return DurationFormatUtils.formatDuration(durationMillis, format, padWithZeros)
        }

        fun formatChartXAxisDayString(timestamp: Long): String {
            val now = Date()
            val mFormat = "LLL"
            val yFormat = if (now.isSameYear(timestamp)) "" else ", yyyy"
            val formatter = DateTimeFormatter.ofPattern(String.format("d\n%s%s", mFormat, yFormat))
            return formatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate())
        }

        fun formatChartXAxisMonthString(month: Int): String {
            var then = Instant.ofEpochMilli(Date().time).atZone(ZoneId.systemDefault()).toLocalDate()
            then = then.withMonth(month)
            val thenMillis = then.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val now = Date()
            val mFormat = "LLL"
            val yFormat = if (now.isSameYear(thenMillis)) "" else ", yyyy"
            val formatter = DateTimeFormatter.ofPattern(String.format("%s%s", mFormat, yFormat))
            return formatter.format(Instant.ofEpochMilli(thenMillis).atZone(ZoneId.systemDefault()).toLocalDate())
        }

        fun formatChartXAxisYearString(year: Int): String {
            var then = Instant.ofEpochMilli(Date().time).atZone(ZoneId.systemDefault()).toLocalDate()
            then = then.withMonth(1)
            then = then.withYear(year)
            val thenMillis = then.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val formatter = DateTimeFormatter.ofPattern(String.format("%d", year))
            return formatter.format(Instant.ofEpochMilli(thenMillis).atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }
}