package com.everlog.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout

class AppBarLayoutNoShadow : AppBarLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        outlineProvider = null
    }
}