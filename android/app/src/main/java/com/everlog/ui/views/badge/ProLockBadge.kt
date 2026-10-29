package com.everlog.ui.views.badge

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.everlog.databinding.ViewBadgeProLockBinding

class ProLockBadge : LinearLayout {

    private lateinit var binding: ViewBadgeProLockBinding

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        setupLayout()
    }

    // Setup

    private fun setupLayout() {
        binding = ViewBadgeProLockBinding.inflate(LayoutInflater.from(context), this, true)
    }
}