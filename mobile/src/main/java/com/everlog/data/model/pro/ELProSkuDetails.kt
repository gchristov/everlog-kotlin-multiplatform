package com.everlog.data.model.pro

import android.content.Context
import com.android.billingclient.api.SkuDetails
import com.everlog.R
import org.threeten.bp.Period
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

data class ELProSkuDetails(

        val duration: Period?,
        val freeTrialDuration: Period?,
        val skuDetails: SkuDetails

) {

    private val MICROS_RATIO = 1000000.0

    companion object {

        @JvmStatic
        fun withSkuDetails(details: SkuDetails): ELProSkuDetails {
            return ELProSkuDetails(
                    Period.parse(details.subscriptionPeriod),
                    Period.parse(details.freeTrialPeriod),
                    details
            )
        }
    }

    fun priceSummary(context: Context): String {
        return String.format("%s %s", durationSummary(context, duration), skuDetails.price)
    }

    fun weeklyPriceSummary() = String.format("%s / WEEK", calculateWeeklyPriceSummary())

    fun freeTrialSummary(context: Context): String? {
        return durationSummary(context, freeTrialDuration)
    }

    fun freeTrialDays(): Int {
        return calculatePeriodDays(freeTrialDuration)
    }

    // Utils

    private fun durationSummary(context: Context, period: Period?): String? {
        return when {
            period == null -> null
            period.years > 0 -> context.resources.getQuantityString(R.plurals.years, period.years, period.years)
            period.months > 0 -> context.resources.getQuantityString(R.plurals.months, period.months, period.months)
            period.days > 0 -> context.resources.getQuantityString(R.plurals.days, period.days, period.days)
            else -> null
        }
    }

    private fun calculateWeeklyPriceSummary(): String {
        // Based on https://developer.android.com/reference/com/android/billingclient/api/SkuDetails
        val price = skuDetails.priceAmountMicros / MICROS_RATIO
        val weeklyPrice = price / (calculatePeriodDays(duration) / 7)
        return formatCurrency(weeklyPrice, skuDetails.priceCurrencyCode)
    }

    private fun calculatePeriodDays(period: Period?): Int {
        var days = 0
        if (period != null) {
            days += period.days
            days += period.months * 30 // 1month = 30weeks
            days += period.years * 365 // 1year = 365weeks
        }
        return days
    }

    private fun formatCurrency(value: Double, currencyCode: String): String {
        val format = NumberFormat.getCurrencyInstance()
        // Set correct currency, show only .00 decimals and do not round
        val currency = Currency.getInstance(currencyCode)
        format.currency = currency
        format.roundingMode = RoundingMode.DOWN
        format.maximumFractionDigits = 2
        return format.format(value)
    }
}