package com.everlog.ui.views.notification.warning

import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.Utils

class PresenterWarningNotification : BaseViewPresenter<MvpViewWarningNotification>() {

    override fun onReady() {
        observeCloseClick()
        observeActionClick()
    }

    // Observers

    private fun observeCloseClick() {
        subscriptions.add(mvpView.onClickClose()
                .compose(applyUISchedulers())
                .subscribe({
                    handleHideNotification(false)
                }) { handleError(it) })
    }

    private fun observeActionClick() {
        subscriptions.add(mvpView.onClickAction()
                .compose(applyUISchedulers())
                .subscribe({ handleShowAction() }) { handleError(it) })
    }

    // Handlers

    private fun handleHideNotification(wait: Boolean) {
        Utils.runWithDelay({
            mvpView?.hideNotification()
        }, if (wait) 500 else 0)
    }

    private fun handleShowAction() {
        when (mvpView.getType()) {
            WarningNotificationView.WarningType.PRO_PLAN_DAYS,
            WarningNotificationView.WarningType.PRO_MUSCLE_GOALS-> navigator.openProBuy()
        }
    }
}