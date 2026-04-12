package com.everlog.ui.activities.login

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewLogin : BaseActivityMvpView {

    fun onClickLogin(): Observable<Void>

    fun onClickLoginGoogle(): Observable<Void>

    fun onClickRegister(): Observable<Void>

    fun onClickRegisterGoogle(): Observable<Void>

    fun onClickTerms(): Observable<Void>

    fun onClickResetPassword(): Observable<Void>

    fun onClickPrivacy(): Observable<Void>

    fun getLoginEmail(): String

    fun getLoginPassword(): String

    fun getRegisterFullName(): String

    fun getRegisterEmail(): String

    fun getRegisterPassword(): String

    fun termsAccepted(): Boolean

    fun newsletterDecided(): Boolean

    fun newsletterAccepted(): Boolean

    fun showGoogleLoading(state: LoginActivity.LoadingState)

    fun showLoginLoading(state: LoginActivity.LoadingState)

    fun showRegisterLoading(state: LoginActivity.LoadingState)

    fun showLoginError(emailError: String?, passwordError: String?)

    fun showRegisterError(nameError: String?,
                          emailError: String?,
                          passwordError: String?)
}