package com.everlog.android.auth.login

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.everlog.android.auth.AuthAppState
import com.everlog.android.auth.AuthRequestCodes
import com.everlog.android.auth.AuthService
import com.everlog.android.auth.R
import com.everlog.android.auth.data.AuthActionListener
import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.auth.login.ValidationUtils
import com.everlog.android.auth.resetpassword.ResetPasswordActivity
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.Utils
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.mvp.dialog.ToastBuilder
import com.everlog.android.common.mvp.util.KeyboardUtils
import com.everlog.android.common.navigation.Home
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.Onboarding
import com.everlog.android.common.navigation.PrivacyPolicy
import com.everlog.android.common.navigation.Terms
import rx.Observable

class PresenterLogin(
    private val auth: AuthService,
    private val navigator: Navigator,
    private val appState: AuthAppState,
) : BaseActivityPresenter<MvpViewLogin>() {

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
            mvpView?.context?.let(appState::cancelAppUseReminder)
        }, 10)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        auth.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthRequestCodes.ONBOARDING) {
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
                    navigator.open(mvpView.context, Terms(mvpView.context.getString(DesignR.string.login_terms)))
                }) { throwable -> handleError(throwable) })
    }

    private fun observePrivacyClick() {
        subscriptions.add(mvpView.onClickPrivacy()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.open(mvpView.context, PrivacyPolicy(mvpView.context.getString(DesignR.string.login_terms_privacy)))
                }) { throwable -> handleError(throwable) })
    }

    private fun observeResetPasswordClick() {
        subscriptions.add(mvpView.onClickResetPassword()
                .compose(applyUISchedulers())
                .subscribe({
                    mvpView?.context?.let { it.startActivity(ResetPasswordActivity.launchIntent(it)) }
                }) { throwable -> handleError(throwable) })
    }


    // Handlers

    private fun handleLoginGoogle() {
        mvpView?.showGoogleLoading(LoginActivity.LoadingState.LOADING)
        auth.loginWithGoogle(mvpView.getActivity()!!, object : AuthActionListener() {
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
            auth.login(email, password, object : AuthActionListener() {
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
        auth.loginAnonymously(object : AuthActionListener() {
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
                KeyboardUtils.hideKeyboard(mvpView.getActivity())
                mvpView.showRegisterLoading(LoginActivity.LoadingState.LOADING)
                auth.register(name, email, password, object : AuthActionListener() {
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
                mvpView?.showToast(R.string.login_terms_not_accepted)
            }
        } else {
            mvpView?.showRegisterError(nameError, emailError, passwordError)
        }
    }

    // Saved even if the screen has gone, so the next launch still opens onboarding (see PresenterSplash)
    private fun saveOnboardingPending(newUser: Boolean) {
        if (newUser) {
            appState.setOnboardingPending(true)
        }
    }

    // RESULT_OK whether the user set up or skipped. Anything else means onboarding was closed without
    // finishing, e.g. the system removed it after the app died, so it's still to do, unless it was
    // finished from another screen in the meantime (see PresenterSplash).
    private fun onOnboardingResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK && appState.onboardingPending()) {
            mvpView?.getActivity()?.let { navigator.open(it, Onboarding) }
        } else {
            appState.setOnboardingPending(false)
            mvpView?.getActivity()?.let { navigator.open(it, Home) }
        }
    }

    // A new account goes through onboarding first, then home (see onOnboardingResult)
    private fun continueAfterLogin(newUser: Boolean) {
        if (newUser) {
            mvpView?.getActivity()?.let { navigator.open(it, Onboarding) }
        } else {
            mvpView?.getActivity()?.let { navigator.open(it, Home) }
        }
    }
}
