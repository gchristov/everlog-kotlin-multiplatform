package com.everlog.ui.activities.base

import android.app.Activity
import android.content.Intent
import com.everlog.ui.dialog.DialogBuilder.AppBlockerDialogType
import com.everlog.ui.mvp.BaseMvpView
import rx.Observable

interface BaseActivityMvpView : BaseMvpView {

    fun getActivity(): Activity?

    fun showPrompt(title: String,
                   message: String,
                   yes: String,
                   no: String): Observable<Int>

    fun showPrompt(titleResId: Int,
                   messageResId: Int,
                   yesResId: Int,
                   noResId: Int): Observable<Int>

    fun showOKPrompt(titleResId: Int, messageResId: Int): Observable<Void>

    fun showOKPrompt(title: String, message: String): Observable<Void>

    fun showAppBlockerPrompt(type: AppBlockerDialogType?): Observable<Int>

    fun showOK(titleResId: Int, messageResId: Int)

    fun showToast(messageResId: Int)

    fun showToast(message: String)

    fun showLongToast(messageResId: Int)

    fun showLongToast(message: String)

    fun setViewResult(code: Int)

    fun setViewResult(code: Int, intent: Intent)

    fun closeScreen()

    fun toggleLoadingOverlay(show: Boolean, message: String?)

    fun toggleLoadingOverlay(show: Boolean)
}