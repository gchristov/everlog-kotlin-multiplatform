package com.everlog.ui.fragments.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.dialog.TaskDialog
import com.everlog.ui.dialog.ToastBuilder
import rx.Observable

abstract class BaseFragment : Fragment(), BaseFragmentMvpView {

    protected abstract fun onFragmentCreated()

    protected abstract fun getLayoutResId(): Int

    open fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? = null

    protected abstract fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>?

    protected abstract fun setupPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bindingView = getBindingView(inflater, container)
        return bindingView ?: (inflater.inflate(getLayoutResId(), container, false) as ViewGroup)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPresenter()
        getPresenter<BaseFragmentMvpView>()?.onRestoreInstanceState(savedInstanceState)
        getPresenter<BaseFragmentMvpView>()?.attachView(this)
        getPresenter<BaseFragmentMvpView>()?.init()
        onFragmentCreated()
        getPresenter<BaseFragmentMvpView>()?.onReady()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        getPresenter<BaseFragmentMvpView>()?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        getPresenter<BaseFragmentMvpView>()?.detachView()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        getPresenter<BaseFragmentMvpView>()?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        getPresenter<BaseFragmentMvpView>()?.onFragmentPaused()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        getPresenter<BaseFragmentMvpView>()?.onFragmentResumed()
    }

    override fun getParentActivity(): FragmentActivity? {
        return activity
    }

    override fun showPrompt(title: String?, message: String?, yes: String?, no: String?): Observable<Int> {
        return DialogBuilder.showPrompt(getParentActivity(), title, message, yes, no)
    }

    override fun showPrompt(titleResId: Int, messageResId: Int, yesResId: Int, noResId: Int): Observable<Int> {
        return showPrompt(getString(titleResId), getString(messageResId), getString(yesResId), getString(noResId))
    }

    override fun showOK(titleResId: Int, messageResId: Int) {
        showOKPrompt(titleResId, messageResId)
    }

    override fun showOKPrompt(titleResId: Int, messageResId: Int): Observable<Void> {
        return showOKPrompt(getString(titleResId), getString(messageResId))
    }

    override fun showOKPrompt(title: String?, message: String?): Observable<Void> {
        return DialogBuilder.showOKPrompt(getParentActivity(), title, message)
    }

    override fun showAppBlockerPrompt(type: DialogBuilder.AppBlockerDialogType): Observable<Int> {
        return DialogBuilder.showAppBlockerDialog(getParentActivity(), type)
    }

    override fun showToast(messageResId: Int) {
        ToastBuilder.showToast(getParentActivity(), messageResId)
    }

    override fun showLongToast(messageResId: Int) {
        ToastBuilder.showToast(getParentActivity(), getString(messageResId), true)
    }

    override fun toggleLoadingOverlay(show: Boolean, message: String?) {
        if (show) {
            if (message == null) {
                TaskDialog.getInstance().showProcessingDialog(activity)
            } else {
                TaskDialog.getInstance().showDialog(message, activity)
            }
        } else {
            TaskDialog.getInstance().hideDialog()
        }
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleLoadingOverlay(show, null)
    }
}