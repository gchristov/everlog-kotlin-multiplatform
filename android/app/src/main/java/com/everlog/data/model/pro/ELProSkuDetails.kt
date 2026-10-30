package com.everlog.data.model.pro

import com.android.billingclient.api.SkuDetails
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

    fun weeklyPriceSummary() = String.format("%s / WEEK", calculateWeeklyPriceSummary())

    fun freeTrialDays(): Int {
        return calculatePeriodDays(freeTrialDuration)
    }

    // Utils

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