package com.everlog.android.plan.congratulate

import com.everlog.android.common.mvp.activity.BaseActivityPresenter

class PresenterCongratulate : BaseActivityPresenter<MvpViewCongratulate>() {

    override fun onReady() {
        observeActionClick()
    }

    // Observers

    private fun observeActionClick() {
        subscriptions.add(mvpView.onClickAction()
                .compose(applyUISchedulers())
                .subscribe {
                    mvpView.closeScreen()
                })
    }
}