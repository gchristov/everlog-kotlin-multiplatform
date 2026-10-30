package com.everlog.android.common.mvp.fragment

import androidx.fragment.app.FragmentActivity
import com.everlog.android.common.mvp.BaseMvpView
import rx.Observable

interface BaseFragmentMvpView : BaseMvpView {

    fun getParentActivity(): FragmentActivity?

    fun showPrompt(title: String?, message: String?, yes: String?, no: String?): Observable<Int>

    fun showPrompt(titleResId: Int, messageResId: Int, yesResId: Int, noResId: Int): Observable<Int>

    /**
     * See [com.everlog.android.common.mvp.dialog.DialogBuilder.showDestructivePrompt].
     */
    fun showDestructivePrompt(titleResId: Int, messageResId: Int, actionResId: Int, cancelResId: Int): Observable<Int>

    fun showOKPrompt(titleResId: Int, messageResId: Int): Observable<Void>

    fun showOKPrompt(title: String?, message: String?): Observable<Void>

    fun showOK(titleResId: Int, messageResId: Int)

    fun showToast(messageResId: Int)

    fun showLongToast(messageResId: Int)

    fun toggleLoadingOverlay(show: Boolean, message: String?)

    fun toggleLoadingOverlay(show: Boolean)
}