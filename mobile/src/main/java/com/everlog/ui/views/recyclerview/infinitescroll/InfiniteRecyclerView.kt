package com.everlog.ui.views.recyclerview.infinitescroll

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import rx.Observable
import rx.subjects.PublishSubject

class InfiniteRecyclerView : RecyclerView {

    private val mLoadMorePublisher = PublishSubject.create<Void>()
    private var mLoading = false
    private var mInfiniteEnabled = true

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setupScroll()
    }

    fun observeLoadMore(): Observable<Void> {
        return mLoadMorePublisher
    }

    fun finishLoading() {
        mLoading = false
    }

    fun toggleInfiniteLoadingEnabled(value: Boolean) {
        mInfiniteEnabled = value
    }

    private fun isAtBottom(): Boolean {
        var isAtBottom = false
        if (layoutManager != null) {
            val total = layoutManager!!.itemCount
            if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
                val manager = layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                isAtBottom = manager.findLastVisibleItemPosition() >= total - 1
            } else if (layoutManager is androidx.recyclerview.widget.StaggeredGridLayoutManager) {
                val manager = layoutManager as androidx.recyclerview.widget.StaggeredGridLayoutManager
                var lastVisibleItems: IntArray? = null
                lastVisibleItems = manager.findLastVisibleItemPositions(lastVisibleItems)
                if (lastVisibleItems != null && lastVisibleItems.isNotEmpty()) {
                    for (col in lastVisibleItems) {
                        if (col >= total - 1) {
                            isAtBottom = true
                            break
                        }
                    }
                }

            }
        }
        return isAtBottom
    }

    // Setup

    private fun setupScroll() {
        addOnScrollListener(object : OnScrollListener() {

            override fun onScrolled(@NonNull recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isAtBottom() && mInfiniteEnabled) {
                    if (!mLoading) {
                        mLoading = true
                        mLoadMorePublisher.onNext(null)
                    }
                }
            }
        })
    }
}