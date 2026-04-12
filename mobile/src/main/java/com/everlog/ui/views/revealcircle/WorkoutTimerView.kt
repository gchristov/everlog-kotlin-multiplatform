package com.everlog.ui.views.revealcircle

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.databinding.ViewWorkoutTimerBinding
import com.everlog.utils.format.FormatUtils.Companion.formatDurationShort

class WorkoutTimerView(context: Context,
                       layoutId: Int? = R.layout.view_workout_timer,
                       layoutParams: ViewGroup.LayoutParams? = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) : BaseRevealCircleView(context, layoutId!!, layoutParams) {

    private val TAG = "WorkoutTimerView"

    private lateinit var binding: ViewWorkoutTimerBinding

    override fun setupLayout(layoutId: Int) {
        binding = ViewWorkoutTimerBinding.inflate(LayoutInflater.from(context), this, true)
        this.tag = tag()
    }

    override fun tag(): String {
        return TAG
    }

    override fun onReady() {
        // No-op
    }

    fun observeIncreaseClick(listener: OnClickListener) {
        binding.increaseBtn.setOnClickListener(listener)
    }

    fun observeDecreaseClick(listener: OnClickListener) {
        binding.decreaseBtn.setOnClickListener(listener)
    }

    fun observeCancelClick(listener: OnClickListener) {
        binding.cancelBtn.setOnClickListener(listener)
    }

    fun updateTime(title: String,
                   timeRemainingSeconds: Int,
                   progress: Int) {
        if (timeRemainingSeconds >= 0) {
            binding.titleLbl.text = title
            binding.timeField.text = formatDurationShort(timeRemainingSeconds * 1000.toLong(), "mm:ss")
            binding.progressBar.progress = progress
        }
    }
}