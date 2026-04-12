package com.everlog.ui.views.badge

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.everlog.databinding.ViewBadgeNewBinding

class NewBadge : LinearLayout {

    private lateinit var binding: ViewBadgeNewBinding

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        setupLayout()
    }

    // Setup

    private fun setupLayout() {
        binding = ViewBadgeNewBinding.inflate(LayoutInflater.from(context), this, true)
    }
}