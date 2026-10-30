package com.everlog.utils.format

import android.content.Context
import com.everlog.R
import com.everlog.data.model.pro.ELProSkuDetails
import org.threeten.bp.Period

fun ELProSkuDetails.priceSummary(context: Context): String {
    return String.format("%s %s", durationSummary(context, duration), skuDetails.price)
}

private fun durationSummary(context: Context, period: Period?): String? {
    return when {
        period == null -> null
        period.years > 0 -> context.resources.getQuantityString(R.plurals.years, period.years, period.years)
        period.months > 0 -> context.resources.getQuantityString(R.plurals.months, period.months, period.months)
        period.days > 0 -> context.resources.getQuantityString(R.plurals.days, period.days, period.days)
        else -> null
    }
}
