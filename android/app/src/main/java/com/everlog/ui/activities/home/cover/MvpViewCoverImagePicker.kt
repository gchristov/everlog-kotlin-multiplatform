package com.everlog.ui.activities.home.cover

import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.views.recyclerview.infinitescroll.InfiniteRecyclerView
import rx.Observable

interface MvpViewCoverImagePicker : BaseActivityMvpView {

    fun onClickEmptyAction(): Observable<Void>

    fun onLoadMore(): Observable<Void>

    fun toggleEmptyState(visible: Boolean)

    fun getInfiniteList(): InfiniteRecyclerView
}