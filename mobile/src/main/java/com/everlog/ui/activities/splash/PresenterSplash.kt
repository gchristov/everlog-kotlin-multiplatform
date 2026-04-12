package com.everlog.ui.activities.splash

import com.everlog.data.model.ELUser
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.auth.AuthManager.OnAuthActionListener
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.Utils

class PresenterSplash : BaseActivityPresenter<MvpViewSplash>() {

    var isReady = false
        private set

    override fun onReady() {
        checkAccountStatus()
    }

    private fun checkAccountStatus() {
        AuthManager.initialize(object : OnAuthActionListener() {
            override fun onError(throwable: Throwable) {
                isReady = true
                checkAppStatus()
            }

            override fun onSuccess(user: ELUser) {
                isReady = true
                checkAppStatus()
            }

            override fun onLogout() {
                isReady = true
                checkAppStatus()
            }
        })
    }

    private fun checkAppStatus() {
        // TODO: Implement migrations
//        if (!DataMigrationManager.manager.checkMigrations()) {
            checkTutorialStatus()
            checkLoginStatus()
//        } else {
//
//        }
    }

    private fun checkLoginStatus() {
        if (AuthManager.isLoggedIn) {
            navigator.openHome()
        } else {
            navigator.openLogin()
        }
    }

    private fun checkTutorialStatus() {
        if (AuthManager.isLoggedIn) {
            // TODO: Apply actions needed for logged in users
        } else {
            // TODO: Apply actions needed for non-logged in users
        }
    }
}
