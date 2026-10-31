package com.everlog.ui.activities.home.integration

import com.everlog.android.common.design.R as DesignR
import android.content.DialogInterface
import com.everlog.R
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.managers.integrations.GoogleFitIntegrationManager
import com.everlog.android.common.mvp.activity.BaseActivityPresenter

class PresenterIntegration : BaseActivityPresenter<MvpViewIntegration>() {

    override fun onReady() {
        observeDisconnectClick()
        loadData()
    }

    // Observers

    private fun observeDisconnectClick() {
        subscriptions.add(mvpView.onClickDisconnect()
                .compose(applyUISchedulers())
                .subscribe({
                    observeDisconnectConfirm(mvpView.getItemToEdit())
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeDisconnectConfirm(integration: ELIntegration) {
        subscriptions.add(mvpView.showPrompt(integration.convertedType()?.getTitle() ?: "Unknown Integration",
                mvpView.context.getString(R.string.integrations_disconnect_prompt),
                mvpView.context.getString(R.string.integrations_disconnect),
                mvpView.context.getString(DesignR.string.cancel))
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        GoogleFitIntegrationManager.disconnect(integration)
                        mvpView?.closeScreen()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    // Loading

    private fun loadData() {
        mvpView?.showData(mvpView.getItemToEdit())
    }
}