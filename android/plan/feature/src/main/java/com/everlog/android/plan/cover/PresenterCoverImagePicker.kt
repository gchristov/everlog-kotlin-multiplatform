package com.everlog.android.plan.cover

import android.app.Activity
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.plan.data.cover.ApiManager
import com.everlog.android.plan.data.model.cover.CoverImage
import com.everlog.android.plan.data.model.cover.CoverImagesResponse
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.plan.PlanExtras
import com.everlog.android.plan.cover.CoverImageAdapter
import com.everlog.android.plan.cover.InfiniteRecyclerAdapter

class PresenterCoverImagePicker : BaseActivityPresenter<MvpViewCoverImagePicker>() {

    private var RESULTS_PER_PAGE = 20

    private val mAdapter = InfiniteRecyclerAdapter()
    private val mDataListManager = DataListManager<CoverImage>(mAdapter)
    private var mPage = 1

    override fun init() {
        super.init()
        setupListView()
    }

    override fun onReady() {
        observeLoadMore()
        observeEmptyActionClick()
        loadCoverImages()
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Observers

    private fun observeEmptyActionClick() {
        subscriptions.add(mvpView.onClickEmptyAction()
                .compose(applyUISchedulers())
                .subscribe {
                    mvpView?.toggleEmptyState(false)
                    loadCoverImages()
                })
    }

    private fun observeLoadMore() {
        subscriptions.add(mvpView.onLoadMore()
                .compose(applyUISchedulers())
                .subscribe {
                    mPage++
                    loadCoverImages()
                })
    }

    // Loading

    private fun loadCoverImages() {
        if (mDataListManager.isEmpty) {
            mvpView?.toggleLoadingOverlay(true)
        } else {
            // Add loading row.
            addLoadingRow()
        }
        subscriptions.add(ApiManager.coverImagesApi().getImages("workout", RESULTS_PER_PAGE, mPage)!!
                .compose(applySchedulers())
                .subscribe ({response ->
                    mvpView?.toggleLoadingOverlay(false)
                    handleCoverImages(response)
                    removeLoadingRow()
                }, {throwable ->
                    mvpView?.toggleLoadingOverlay(false)
                    handleError(throwable)
                    handleCheckEmptyState()
                    removeLoadingRow()
                }))
    }

    private fun addLoadingRow() {
        mDataListManager.add(null)
    }

    private fun removeLoadingRow() {
        mDataListManager.remove(null)
        mvpView?.getInfiniteList()?.finishLoading()
    }

    // Handlers

    private fun handleCoverImages(response: CoverImagesResponse) {
        mvpView?.getInfiniteList()?.toggleInfiniteLoadingEnabled(response.results.size >= RESULTS_PER_PAGE)
        mDataListManager.addAll(response.results)
        handleCheckEmptyState()
    }

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager.isEmpty)
    }

    private fun handleImageSelected(image: CoverImage) {
        val i = Intent()
        i.putExtra(PlanExtras.COVER_IMAGE, image.getUrl())
        mvpView.setViewResult(Activity.RESULT_OK, i)
        mvpView.closeScreen()
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        val binder = CoverImageAdapter.Binder { item, _ -> handleImageSelected(item) }
        mAdapter.registerBinder(binder)
    }
}