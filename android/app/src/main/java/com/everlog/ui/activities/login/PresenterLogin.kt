package com.everlog.ui.activities.login

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.dialog.ToastBuilder
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import com.everlog.utils.input.ValidationUtils
import rx.Observable

class PresenterLogin : BaseActivityPresenter<MvpViewLogin>() {

    private val DELAY_SUCCESS = 300

    override fun onReady() {
        observeGoogleClick()
        observeLoginClick()
        observeGetStartedClick()
        observeRegisterClick()
        observeTermsClick()
        observePrivacyClick()
        observeResetPasswordClick()
        // APP STARTUP: Delay to not block
        Utils.runWithDelay({
            // Make sure to cancel the app use notification if we're not logged in
            navigator.cancelAppUseNotification()
        }, 10)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AuthManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_ONBOARDING) {
            onOnboardingResult(resultCode)
        }
    }

    // Observers

    private fun observeGoogleClick() {
        subscriptions.add(Observable.merge(mvpView.onClickLoginGoogle(), mvpView.onClickRegisterGoogle())
                .compose(applyUISchedulers())
                .subscribe({
                    handleLoginGoogle()
                }) { throwable -> handleError(throwable) })
    }

    private fun observeLoginClick() {
        subscriptions.add(mvpView.onClickLogin()
                .compose(applyUISchedulers())
                .subscribe({
                    handleLogin()
                }) { throwable -> handleError(throwable) })
    }

    private fun observeGetStartedClick() {
        subscriptions.add(mvpView.onClickGetStarted()
                .compose(applyUISchedulers())
                .subscribe({
                    handleLoginAnonymous()
                }) { throwable -> handleError(throwable) })
    }

    private fun observeRegisterClick() {
        subscriptions.add(mvpView.onClickRegister()
                .compose(applyUISchedulers())
                .subscribe({
                    handleRegister()
                }) { throwable -> handleError(throwable) })
    }

    private fun observeTermsClick() {
        subscriptions.add(mvpView.onClickTerms()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openWebView(ELConstants.URL_TERMS, mvpView.context.getString(R.string.login_terms))
                }) { throwable -> handleError(throwable) })
    }

    private fun observePrivacyClick() {
        subscriptions.add(mvpView.onClickPrivacy()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openWebView(ELConstants.URL_PRIVACY, mvpView.context.getString(R.string.login_terms_privacy))
                }) { throwable -> handleError(throwable) })
    }

    private fun observeResetPasswordClick() {
        subscriptions.add(mvpView.onClickResetPassword()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openResetPassword()
                }) { throwable -> handleError(throwable) })
    }


    // Handlers

    private fun handleLoginGoogle() {
        mvpView?.showGoogleLoading(LoginActivity.LoadingState.LOADING)
        AuthManager.loginWithGoogle(navigator, object : AuthManager.OnAuthActionListener() {
            override fun onSuccess(user: ELUser, newUser: Boolean) {
                saveOnboardingPending(newUser)
                if (isAttachedToView) {
                    mvpView?.showGoogleLoading(LoginActivity.LoadingState.DONE)
                    Utils.runWithDelay({ continueAfterLogin(newUser) }, DELAY_SUCCESS)
                }
            }

            override fun onError(throwable: Throwable) {
                if (isAttachedToView) {
                    mvpView?.showGoogleLoading(LoginActivity.LoadingState.DEFAULT)
                    ToastBuilder.showToast(mvpView.context, throwable.message, true)
                }
            }
        })
    }

    private fun handleLogin() {
        val email = mvpView.getLoginEmail()
        val password = mvpView.getLoginPassword()
        val emailError = ValidationUtils.validateEmail(mvpView.context, email)
        val passwordError = ValidationUtils.validatePassword(mvpView.context, password)
        if (TextUtils.isEmpty(emailError)
                && TextUtils.isEmpty(passwordError)) {
            KeyboardUtils.hideKeyboard(mvpView.getActivity())
            mvpView?.showLoginLoading(LoginActivity.LoadingState.LOADING)
            AuthManager.login(email, password, object : AuthManager.OnAuthActionListener() {
                override fun onSuccess(user: ELUser, newUser: Boolean) {
                    saveOnboardingPending(newUser)
                    if (isAttachedToView) {
                        mvpView?.showLoginLoading(LoginActivity.LoadingState.DONE)
                        Utils.runWithDelay({ continueAfterLogin(newUser) }, DELAY_SUCCESS)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (isAttachedToView) {
                        mvpView?.showLoginLoading(LoginActivity.LoadingState.DEFAULT)
                        ToastBuilder.showToast(mvpView.context, throwable.message, true)
                    }
                }
            })
        } else {
            mvpView?.showLoginError(emailError, passwordError)
        }
    }

    private fun handleLoginAnonymous() {
        mvpView?.showGetStartedLoading(LoginActivity.LoadingState.LOADING)
        AuthManager.loginAnonymously(object : AuthManager.OnAuthActionListener() {
            override fun onSuccess(user: ELUser, newUser: Boolean) {
                saveOnboardingPending(newUser)
                if (isAttachedToView) {
                    mvpView?.showGetStartedLoading(LoginActivity.LoadingState.DONE)
                    Utils.runWithDelay({ continueAfterLogin(newUser) }, DELAY_SUCCESS)
                }
            }

            override fun onError(throwable: Throwable) {
                if (isAttachedToView) {
                    mvpView?.showGetStartedLoading(LoginActivity.LoadingState.DEFAULT)
                    ToastBuilder.showToast(mvpView.context, throwable.message, true)
                }
            }
        })
    }

    private fun handleRegister() {
        val name = mvpView.getRegisterFullName()
        val email = mvpView.getRegisterEmail()
        val password = mvpView.getRegisterPassword()
        val nameError = ValidationUtils.validateName(mvpView.context, name)
        val emailError = ValidationUtils.validateEmail(mvpView.context, email)
        val passwordError = ValidationUtils.validatePassword(mvpView.context, password)
        if (TextUtils.isEmpty(nameError)
                && TextUtils.isEmpty(emailError)
                && TextUtils.isEmpty(passwordError)) {
            if (mvpView?.termsAccepted() == true) {
                if (mvpView?.newsletterDecided() == true) {
                    KeyboardUtils.hideKeyboard(mvpView.getActivity())
                    mvpView.showRegisterLoading(LoginActivity.LoadingState.LOADING)
                    AuthManager.register(name, email, password, mvpView.newsletterAccepted(), object : AuthManager.OnAuthActionListener() {
                        override fun onSuccess(user: ELUser, newUser: Boolean) {
                            saveOnboardingPending(newUser)
                            if (isAttachedToView) {
                                mvpView?.showRegisterLoading(LoginActivity.LoadingState.DONE)
                                Utils.runWithDelay({ continueAfterLogin(newUser) }, DELAY_SUCCESS)
                            }
                        }

                        override fun onError(throwable: Throwable) {
                            if (isAttachedToView) {
                                mvpView.showRegisterLoading(LoginActivity.LoadingState.DEFAULT)
                                ToastBuilder.showToast(mvpView.context, throwable.message, true)
                            }
                        }
                    })
                } else {
                    mvpView?.showToast(R.string.notifications_newsletter_not_accepted)
                }
            } else {
                mvpView?.showToast(R.string.login_terms_not_accepted)
            }
        } else {
            mvpView?.showRegisterError(nameError, emailError, passwordError)
        }
    }

    // Saved even if the screen has gone, so the next launch still opens onboarding (see PresenterSplash)
    private fun saveOnboardingPending(newUser: Boolean) {
        if (newUser) {
            SettingsManager.manager.setOnboardingPending(true)
        }
    }

    // RESULT_OK whether the user set up or skipped. Anything else means onboarding was closed without
    // finishing, e.g. the system removed it after the app died, so it's still to do, unless it was
    // finished from another screen in the meantime (see PresenterSplash).
    private fun onOnboardingResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK && SettingsManager.manager.onboardingPending()) {
            navigator.openOnboarding()
        } else {
            SettingsManager.manager.setOnboardingPending(false)
            navigator.openHome()
        }
    }

    // A new account goes through onboarding first, then home (see onOnboardingResult)
    private fun continueAfterLogin(newUser: Boolean) {
        if (newUser) {
            navigator.openOnboarding()
        } else {
            navigator.openHome()
        }
    }
}
