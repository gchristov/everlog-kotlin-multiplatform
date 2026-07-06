package com.everlog.ui.views.revealcircle

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.everlog.R
import com.everlog.utils.format.FormatUtils.Companion.formatDurationShort

class WorkoutTimerView(context: Context,
                       layoutId: Int? = R.layout.view_workout_timer,
                       layoutParams: ViewGroup.LayoutParams? = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) : BaseRevealCircleView(context, layoutId!!, layoutParams) {

    private val TAG = "WorkoutTimerView"

    // Both view_workout_timer.xml and view_workout_timer_compact.xml share these ids,
    // so the layout requested via the constructor's layoutId is honoured (setupLayout
    // is not overridden here, relying on the base class's generic View.inflate).
    private val titleLbl: TextView by lazy { findViewById(R.id.titleLbl) }
    private val timeField: TextView by lazy { findViewById(R.id.timeField) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    private val cancelBtn: View by lazy { findViewById(R.id.cancelBtn) }
    private val increaseBtn: View by lazy { findViewById(R.id.increaseBtn) }
    private val decreaseBtn: View by lazy { findViewById(R.id.decreaseBtn) }

    override fun tag(): String {
        return TAG
    }

    override fun onReady() {
        // No-op
    }

    fun observeIncreaseClick(listener: OnClickListener) {
        increaseBtn.setOnClickListener(listener)
    }

    fun observeDecreaseClick(listener: OnClickListener) {
        decreaseBtn.setOnClickListener(listener)
    }

    fun observeCancelClick(listener: OnClickListener) {
        cancelBtn.setOnClickListener(listener)
    }

    fun updateTime(title: String,
                   timeRemainingSeconds: Int,
                   progress: Int) {
        if (timeRemainingSeconds >= 0) {
            titleLbl.text = title
            timeField.text = formatDurationShort(timeRemainingSeconds * 1000.toLong(), "mm:ss")
            progressBar.progress = progress
        }
    }
}