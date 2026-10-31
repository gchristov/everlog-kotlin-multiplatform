package com.everlog.android.plan.cover

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import com.everlog.android.plan.cover.InfiniteRecyclerView
import rx.Observable

interface MvpViewCoverImagePicker : BaseActivityMvpView {

    fun onClickEmptyAction(): Observable<Void>

    fun onLoadMore(): Observable<Void>

    fun toggleEmptyState(visible: Boolean)

    fun getInfiniteList(): InfiniteRecyclerView
}