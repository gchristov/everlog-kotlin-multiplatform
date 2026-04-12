package com.everlog.ui.views.badge

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.everlog.R
import com.everlog.databinding.ViewBadgeProBinding

class ProBadge : LinearLayout {

    private lateinit var binding: ViewBadgeProBinding

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
        setupLayout(attrs, defStyle)
    }

    // Setup

    private fun setupLayout(attrs: AttributeSet?, defStyleAttr: Int) {
        binding = ViewBadgeProBinding.inflate(LayoutInflater.from(context), this, true)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProBadge, defStyleAttr, 0)
            try {
                val hasTextSize = typedArray.hasValue(R.styleable.ProBadge_pbTextSize)
                if (hasTextSize) {
                    binding.badgeText.setTextSize(TypedValue.COMPLEX_UNIT_PX, typedArray.getDimensionPixelSize(R.styleable.ProBadge_pbTextSize, 8).toFloat())
                }
            } finally {
                typedArray.recycle()
            }
        }
    }
}