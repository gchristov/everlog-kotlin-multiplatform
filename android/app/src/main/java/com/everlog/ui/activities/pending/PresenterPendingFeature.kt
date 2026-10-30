package com.everlog.ui.activities.pending

import com.everlog.android.common.mvp.activity.BaseActivityPresenter

abstract class PresenterPendingFeature<T: MvpViewPendingFeature> : BaseActivityPresenter<T>() {

    override fun onReady() {
        observeEmptyActionClick()
    }

    // Observers

    private fun observeEmptyActionClick() {
        subscriptions.add(mvpView.onClickEmptyAction()
                .compose(applyUISchedulers())
                .subscribe {
                    mvpView.closeScreen()
                })
    }
}