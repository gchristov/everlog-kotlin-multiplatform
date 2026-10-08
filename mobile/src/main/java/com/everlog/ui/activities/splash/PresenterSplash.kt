package com.everlog.ui.activities.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.auth.AuthManager.OnAuthActionListener
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.Utils

class PresenterSplash : BaseActivityPresenter<MvpViewSplash>() {

    var isReady = false
        private set

    // Onboarding is open on top. If the app was closed in the meantime, this screen is rebuilt when
    // onboarding finishes and only waits for its result.
    private var openedOnboarding = false

    override fun onRestoreInstanceState(inState: Bundle?) {
        super.onRestoreInstanceState(inState)
        openedOnboarding = inState?.getBoolean(STATE_OPENED_ONBOARDING) == true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_OPENED_ONBOARDING, openedOnboarding)
    }

    override fun onReady() {
        // Opened from the launcher on top of the app's existing screens, which happens after the app
        // died with a screen open. Those screens pick up where they were instead.
        if (mvpView.isLaunchedOverApp()) {
            isReady = true
            mvpView.closeScreen()
            return
        }
        if (openedOnboarding) {
            isReady = true
            return
        }
        checkAccountStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_ONBOARDING) {
            // RESULT_OK whether the user set up or skipped. Anything else means onboarding was closed
            // without finishing, e.g. the system removed it after the app died, so it's still to do,
            // unless it was finished from another screen in the meantime (see PresenterLogin).
            if (resultCode != Activity.RESULT_OK && SettingsManager.manager.onboardingPending()) {
                navigator.openOnboarding()
            } else {
                openedOnboarding = false
                SettingsManager.manager.setOnboardingPending(false)
                navigator.openHome()
            }
        }
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
        when {
            !AuthManager.isLoggedIn -> navigator.openLogin()
            // A new account whose onboarding was interrupted, e.g. by closing the app, picks it up again
            SettingsManager.manager.onboardingPending() -> {
                openedOnboarding = true
                navigator.openOnboarding()
            }
            else -> navigator.openHome()
        }
    }

    private fun checkTutorialStatus() {
        if (AuthManager.isLoggedIn) {
            // TODO: Apply actions needed for logged in users
        } else {
            // TODO: Apply actions needed for non-logged in users
        }
    }

    private companion object {
        const val STATE_OPENED_ONBOARDING = "STATE_OPENED_ONBOARDING"
    }
}
