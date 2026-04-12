package com.everlog.ui.activities.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.dialog.DialogBuilder.AppBlockerDialogType
import com.everlog.ui.dialog.TaskDialog
import com.everlog.ui.dialog.ToastBuilder
import com.everlog.utils.ActivityUtils
import com.everlog.utils.input.KeyboardUtils
import com.facebook.shimmer.Shimmer.ColorHighlightBuilder
import com.facebook.shimmer.ShimmerFrameLayout
import rx.Observable

abstract class BaseActivity : AppCompatActivity(), BaseActivityMvpView {

    protected abstract fun onActivityCreated()

    protected abstract fun getLayoutResId(): Int

    open fun getBindingView(): View? = null

    protected abstract fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>?

    protected abstract fun setupPresenter()

    protected abstract fun getAnalyticsScreenName(): String

    companion object {

        @JvmStatic
        fun toggleShimmerLayout(layout: ShimmerFrameLayout?,
                                show: Boolean,
                                affectVisibility: Boolean) {
            toggleShimmerLayout(layout, show, affectVisibility, R.color.background_card)
        }

        @JvmStatic
        fun toggleShimmerLayout(layout: ShimmerFrameLayout?,
                                show: Boolean,
                                affectVisibility: Boolean,
                                colorResId: Int) {
            if (layout != null) {
                if (show) {
                    if (affectVisibility) {
                        layout.visibility = View.VISIBLE
                    }
                    layout.setShimmer(ColorHighlightBuilder()
                            .setBaseAlpha(1f)
                            .setBaseColor(ContextCompat.getColor(layout.context, colorResId))
                            .setHighlightAlpha(0.35f)
                            .build())
                    layout.startShimmer()
                } else {
                    if (affectVisibility) {
                        layout.visibility = View.GONE
                    }
                    layout.setShimmer(null)
                    layout.stopShimmer()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Do NOT use activity instance state because that messes up with the ViewPager on the home screen
        super.onCreate(null)
        if (shouldSetOrientation()) {
            ActivityUtils.setOrientation(this)
        }
        
        val bindingView = getBindingView()
        if (bindingView != null) {
            setContentView(bindingView)
        } else {
            if (getLayoutResId() != 0) {
                setContentView(getLayoutResId())
            }
        }

        setupPresenter()
        getPresenter<BaseActivityMvpView>()?.onRestoreInstanceState(savedInstanceState)
        getPresenter<BaseActivityMvpView>()?.attachView(this)
        getPresenter<BaseActivityMvpView>()?.init()
        onActivityCreated()
        getPresenter<BaseActivityMvpView>()?.onReady()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        getPresenter<BaseActivityMvpView>()?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (getPresenter<BaseActivityMvpView>()?.onBackPressedConsumed() == false) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        getPresenter<BaseActivityMvpView>()?.detachView()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        getPresenter<BaseActivityMvpView>()?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        KeyboardUtils.hideKeyboard(this)
        getPresenter<BaseActivityMvpView>()?.onActivityPaused()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsManager.manager.screenName(this, getAnalyticsScreenName())
        getPresenter<BaseActivityMvpView>()?.onActivityResumed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getContext(): Context {
        return this
    }

    override fun getActivity(): AppCompatActivity? {
        return this
    }

    override fun setViewResult(code: Int) {
        setResult(code)
    }

    override fun setViewResult(code: Int, intent: Intent) {
        setResult(code, intent)
    }

    override fun closeScreen() {
        finish()
    }

    override fun showPrompt(title: String,
                            message: String,
                            yes: String,
                            no: String): Observable<Int> {
        return DialogBuilder.showPrompt(this, title, message, yes, no)
    }

    override fun showPrompt(titleResId: Int,
                            messageResId: Int,
                            yesResId: Int,
                            noResId: Int): Observable<Int> {
        return showPrompt(getString(titleResId), getString(messageResId), getString(yesResId), getString(noResId))
    }

    override fun showOK(titleResId: Int, messageResId: Int) {
        showOKPrompt(titleResId, messageResId)
    }

    override fun showOKPrompt(titleResId: Int, messageResId: Int): Observable<Void> {
        return showOKPrompt(getString(titleResId), getString(messageResId))
    }

    override fun showOKPrompt(title: String, message: String): Observable<Void> {
        return DialogBuilder.showOKPrompt(this, title, message)
    }

    override fun showAppBlockerPrompt(type: AppBlockerDialogType?): Observable<Int> {
        return DialogBuilder.showAppBlockerDialog(this, type)
    }

    override fun showToast(messageResId: Int) {
        ToastBuilder.showToast(this, messageResId)
    }

    override fun showToast(message: String) {
        ToastBuilder.showToast(this, message)
    }

    override fun showLongToast(messageResId: Int) {
        ToastBuilder.showToast(this, getString(messageResId), true)
    }

    override fun showLongToast(message: String) {
        ToastBuilder.showToast(this, message, true)
    }

    override fun toggleLoadingOverlay(show: Boolean, message: String?) {
        if (show) {
            if (message == null) {
                TaskDialog.getInstance().showProcessingDialog(this)
            } else {
                TaskDialog.getInstance().showDialog(message, this)
            }
        } else {
            TaskDialog.getInstance().hideDialog()
        }
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleLoadingOverlay(show, null)
    }

    protected open fun shouldSetOrientation(): Boolean {
        return true
    }
}