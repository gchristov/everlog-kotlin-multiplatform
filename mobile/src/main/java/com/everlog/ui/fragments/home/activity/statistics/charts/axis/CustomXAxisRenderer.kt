package com.everlog.ui.fragments.home.activity.statistics.charts.axis

import android.graphics.Canvas
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomXAxisRenderer(viewPortHandler: ViewPortHandler?, xAxis: XAxis?, trans: Transformer?) : XAxisRenderer(viewPortHandler, xAxis, trans) {

    override fun drawLabel(c: Canvas?,
                           formattedLabel: String?,
                           x: Float,
                           y: Float,
                           anchor: MPPointF?,
                           angleDegrees: Float) {
        val line = formattedLabel?.split("\n")
        if (line != null) {
            Utils.drawXAxisValue(c, line[0], x, y, mAxisLabelPaint, anchor, angleDegrees)
            if (line.size > 1) {
                Utils.drawXAxisValue(c, line[1], x, y + mAxisLabelPaint.textSize, mAxisLabelPaint, anchor, angleDegrees)
            }
        }
    }
}