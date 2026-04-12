package com.everlog.ui.fragments.base

import androidx.fragment.app.FragmentActivity
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.mvp.BaseMvpView
import rx.Observable

interface BaseFragmentMvpView : BaseMvpView {

    fun getParentActivity(): FragmentActivity?

    fun showPrompt(title: String?, message: String?, yes: String?, no: String?): Observable<Int>

    fun showPrompt(titleResId: Int, messageResId: Int, yesResId: Int, noResId: Int): Observable<Int>

    fun showOKPrompt(titleResId: Int, messageResId: Int): Observable<Void>

    fun showOKPrompt(title: String?, message: String?): Observable<Void>

    fun showOK(titleResId: Int, messageResId: Int)

    fun showAppBlockerPrompt(type: DialogBuilder.AppBlockerDialogType): Observable<Int>

    fun showToast(messageResId: Int)

    fun showLongToast(messageResId: Int)

    fun toggleLoadingOverlay(show: Boolean, message: String?)

    fun toggleLoadingOverlay(show: Boolean)
}