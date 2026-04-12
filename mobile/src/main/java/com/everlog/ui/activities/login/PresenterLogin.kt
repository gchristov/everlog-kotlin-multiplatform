package com.everlog.ui.activities.login

import android.content.Intent
import android.text.TextUtils
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.AuthManager
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
            override fun onSuccess(user: ELUser) {
                if (isAttachedToView) {
                    mvpView?.showGoogleLoading(LoginActivity.LoadingState.DONE)
                    Utils.runWithDelay({ navigator.openHome() }, DELAY_SUCCESS)
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
                override fun onSuccess(user: ELUser) {
                    if (isAttachedToView) {
                        mvpView?.showLoginLoading(LoginActivity.LoadingState.DONE)
                        Utils.runWithDelay({ navigator.openHome() }, DELAY_SUCCESS)
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
                        override fun onSuccess(user: ELUser) {
                            if (isAttachedToView) {
                                mvpView?.showRegisterLoading(LoginActivity.LoadingState.DONE)
                                Utils.runWithDelay({ navigator.openHome() }, DELAY_SUCCESS)
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
}