package com.everlog.android.auth.resetpassword

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import com.everlog.android.auth.login.LoginActivity
import rx.Observable

interface MvpViewResetPassword : BaseActivityMvpView {

    fun onClickResetPassword(): Observable<Void>

    fun getResetPasswordEmail(): String

    fun showResetPasswordLoading(state: LoginActivity.LoadingState)

    fun showResetPasswordError(emailError: String)
}