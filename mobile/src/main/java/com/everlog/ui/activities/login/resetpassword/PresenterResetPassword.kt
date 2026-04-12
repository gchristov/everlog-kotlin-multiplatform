package com.everlog.ui.activities.login.resetpassword

import android.text.TextUtils
import com.everlog.R
import com.everlog.managers.auth.AuthManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.login.LoginActivity
import com.everlog.ui.dialog.ToastBuilder
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import com.everlog.utils.input.ValidationUtils

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
            AuthManager.requestPasswordReset(email, object : AuthManager.OnAuthActionListener() {
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