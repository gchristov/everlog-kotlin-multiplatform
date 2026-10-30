package com.everlog.android.common.mvp.view

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.summaryValues
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.setSummary
import kotlin.math.max
import com.everlog.android.common.design.R as DesignR

/**
 * A done set's summary: what was lifted in bold after the "x" or "•", or Skipped in grey italics for a set
 * with nothing logged.
 */
fun renderCompleteSetSummary(context: Context, set: ELSet, unit: WeightUnit, summaryView: TextView?) {
    if (set.isWithoutData()) {
        summaryView?.text = "Skipped"
        summaryView?.setTypeface(null, Typeface.ITALIC)
        summaryView?.setTextColor(ContextCompat.getColor(context, DesignR.color.gray_4))
        return
    }
    val txt = setSummary(context, set.summaryValues(useTemplate = false), unit) ?: ""
    val str = SpannableStringBuilder(txt)
    val index = if (txt.contains("x")) txt.indexOf("x") else txt.indexOf("•")
    val xPos = max(index + 1, 0)
    str.setSpan(StyleSpan(Typeface.BOLD), xPos, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    str.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, DesignR.color.white_base)), xPos, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    str.setSpan(StyleSpan(Typeface.NORMAL), 0, xPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    str.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, DesignR.color.gray_1)), 0, xPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    summaryView?.text = str
    summaryView?.setTypeface(null, Typeface.NORMAL)
}
