package com.everlog.ui.views.revealcircle

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.everlog.R
import com.everlog.utils.ViewUtils
import com.everlog.utils.format.FormatUtils.Companion.formatDurationShort

class WorkoutTimerView(context: Context,
                       layoutId: Int,
                       layoutParams: ViewGroup.LayoutParams) : BaseRevealCircleView(context, layoutId, layoutParams) {

    private val TAG = "WorkoutTimerView"

    // setupLayout is not overridden here, relying on the base class's generic View.inflate
    // to honour the layoutId passed to the constructor.
    private val titleLbl: TextView by lazy { findViewById(R.id.titleLbl) }
    private val timeField: TextView by lazy { findViewById(R.id.timeField) }
    private val separatorLbl: View by lazy { findViewById(R.id.separatorLbl) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    private val cancelBtn: View by lazy { findViewById(R.id.cancelBtn) }
    private val increaseBtn: View by lazy { findViewById(R.id.increaseBtn) }
    private val decreaseBtn: View by lazy { findViewById(R.id.decreaseBtn) }

    override fun tag(): String {
        return TAG
    }

    override fun onReady() {
        // Marquee ellipsize only scrolls once the TextView is selected
        titleLbl.isSelected = true
        // width isn't resolved yet on this very first pass, apply once layout catches up
        post { applyTitleMaxWidth() }
    }

    // titleLbl is plain wrap_content so it hugs short titles (sitting right next to
    // separatorLbl/timeField), but a marquee TextView's own measurement doesn't shrink
    // to content the way a plain ellipsized one does, so we cap its width by hand to
    // whatever room is actually left once the fixed-size siblings are accounted for.
    private fun applyTitleMaxWidth() {
        if (width == 0) {
            return
        }
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        separatorLbl.measure(unspecified, unspecified)
        timeField.measure(unspecified, unspecified)
        val toolbarSize = ViewUtils.getToolbarSize(context)
        val titleMarginStart = resources.getDimensionPixelSize(R.dimen.activity_margin_half)
        val fixedWidth = (toolbarSize * 3) + separatorLbl.measuredWidth + timeField.measuredWidth + titleMarginStart
        val maxWidth = (width - fixedWidth).coerceAtLeast(0)
        if (titleLbl.maxWidth != maxWidth) {
            titleLbl.maxWidth = maxWidth
        }
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
            val timeText = formatDurationShort(timeRemainingSeconds * 1000.toLong(), "mm:ss")
            if (!TextUtils.equals(timeField.text, timeText)) {
                timeField.text = timeText
            }
            // Re-measured every tick since timeField's own width can shift slightly
            // (e.g. "9:59" vs "10:00"), which changes how much room titleLbl actually has.
            applyTitleMaxWidth()
            // Re-setting the same text every tick restarts the marquee scroll from the start
            // (CharSequence.equals on a TextView's internal buffer isn't reliable, hence TextUtils),
            // so only touch it when the title actually changed.
            if (!TextUtils.equals(titleLbl.text, title)) {
                titleLbl.text = title
            }
            progressBar.progress = progress
        }
    }
}