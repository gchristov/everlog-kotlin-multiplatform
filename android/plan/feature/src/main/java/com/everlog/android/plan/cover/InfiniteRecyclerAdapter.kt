package com.everlog.android.plan.cover

import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.plan.cover.InfiniteBinder

class InfiniteRecyclerAdapter : RecyclerAdapter() {

    init {
        setupInfiniteBinder()
    }

    // Setup

    private fun setupInfiniteBinder() {
        registerBinder(InfiniteBinder())
    }
}