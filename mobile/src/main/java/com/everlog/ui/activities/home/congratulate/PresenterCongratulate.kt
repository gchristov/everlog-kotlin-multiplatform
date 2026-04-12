package com.everlog.ui.activities.home.congratulate

import com.everlog.ui.activities.base.BaseActivityPresenter

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