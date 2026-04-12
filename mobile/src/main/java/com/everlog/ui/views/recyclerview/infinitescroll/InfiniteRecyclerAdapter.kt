package com.everlog.ui.views.recyclerview.infinitescroll

import com.ahamed.multiviewadapter.RecyclerAdapter

class InfiniteRecyclerAdapter : RecyclerAdapter() {

    init {
        setupInfiniteBinder()
    }

    // Setup

    private fun setupInfiniteBinder() {
        registerBinder(InfiniteBinder())
    }
}