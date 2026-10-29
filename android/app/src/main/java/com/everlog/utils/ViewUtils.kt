package com.everlog.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.R
import kotlin.math.roundToInt

object ViewUtils {

    fun pxToDp(px: Float): Float {
        val densityDpi = Resources.getSystem().displayMetrics.densityDpi.toFloat()
        return px / (densityDpi / 160f)
    }

    @JvmStatic
    fun dpToPx(dp: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dp * density).roundToInt()
    }

    @JvmStatic
    fun getRawDimension(context: Context, dimenResId: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (context.resources.getDimension(dimenResId) / density).roundToInt()
    }

    @JvmStatic
    fun dpToPxFromRaw(context: Context, dimenResId: Int): Int {
        return dpToPx(getRawDimension(context, dimenResId))
    }

    fun getToolbarSize(context: Context): Int {
        val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
        return styledAttributes.getDimension(0, 0f).toInt()
    }

    fun getStatusBarSize(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun setMargins(v: View, l: Int, t: Int, r: Int, b: Int) {
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = v.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(l, t, r, b)
            v.requestLayout()
        }
    }
}