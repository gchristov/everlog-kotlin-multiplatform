package com.everlog.android.auth.resetpassword

import android.text.TextUtils
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.auth.AuthFeature
import com.everlog.android.auth.R
import com.everlog.android.auth.data.AuthActionListener
import com.everlog.android.auth.login.LoginActivity
import com.everlog.android.common.mvp.dialog.ToastBuilder
import com.everlog.android.common.kotlin.Utils
import com.everlog.android.common.mvp.util.KeyboardUtils
import com.everlog.android.auth.login.ValidationUtils

class PresenterResetPassword : BaseActivityPresenter<MvpViewResetPassword>() {

    override fun onReady() {
        observeResetPasswordClick()
    }

    // Observers

    private fun observeResetPasswordClick() {
        subscriptions.add(mvpView.onClickResetPassword()
                .compose(applyUISchedulers())
                .subscribe({
                    handleResetPassword()
                }) { throwable -> handleError(throwable) })
    }

    // Handlers

    private fun handleResetPassword() {
        val email = mvpView.getResetPasswordEmail()
        val emailError = ValidationUtils.validateEmail(mvpView.context, email)
        if (TextUtils.isEmpty(emailError)) {
            KeyboardUtils.hideKeyboard(mvpView.getActivity())
            mvpView?.showResetPasswordLoading(LoginActivity.LoadingState.LOADING)
            AuthFeature.dependencies.requestPasswordReset(email, object : AuthActionListener() {
                override fun onResetPasswordSuccess() {
                    if (isAttachedToView) {
                        mvpView?.showResetPasswordLoading(LoginActivity.LoadingState.DONE)
                        ToastBuilder.showToast(mvpView.context, mvpView.context.getString(R.string.reset_password_success), true)
                        // Add some delay so the button can resize itself to DONE state
                        Utils.runWithDelay({
                            mvpView?.closeScreen()
                        }, 1000)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (isAttachedToView) {
                        mvpView?.showResetPasswordLoading(LoginActivity.LoadingState.DEFAULT)
                        ToastBuilder.showToast(mvpView.context, throwable.message)
                    }
                }
            })
        } else {
            mvpView?.showResetPasswordError(emailError)
        }
    }
}