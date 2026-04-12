package com.everlog.ui.activities.login.resetpassword

import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.login.LoginActivity
import rx.Observable

interface MvpViewResetPassword : BaseActivityMvpView {

    fun onClickResetPassword(): Observable<Void>

    fun getResetPasswordEmail(): String

    fun showResetPasswordLoading(state: LoginActivity.LoadingState)

    fun showResetPasswordError(emailError: String)
}