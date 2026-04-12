package com.everlog.ui.activities.home.integration

import android.content.DialogInterface
import com.everlog.R
import com.everlog.data.model.ELIntegration
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.integrations.GoogleFitIntegrationManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.navigator.Navigator

class PresenterIntegration : BaseActivityPresenter<MvpViewIntegration>() {

    override fun onReady() {
        observeSyncClick()
        observeUnsyncClick()
        observeDisconnectClick()
        loadData()
    }

    // Observers

    private fun observeSyncClick() {
        subscriptions.add(mvpView.onClickSync()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.integrationSyncRequested()
                    navigator.sendEmail(Navigator.ContactType.INTEGRATION_SYNC, null)
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeUnsyncClick() {
        subscriptions.add(mvpView.onClickUnsync()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.integrationUnsyncRequested()
                    navigator.sendEmail(Navigator.ContactType.INTEGRATION_UNSYNC, null)
                }, { throwable -> handleError(throwable) }))
    }

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
                mvpView.context.getString(R.string.cancel))
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