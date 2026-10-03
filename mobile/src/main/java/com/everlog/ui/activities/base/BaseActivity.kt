package com.everlog.ui.activities.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.auth.LocalUserManager
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.dialog.DialogBuilder.AppBlockerDialogType
import com.everlog.ui.dialog.TaskDialog
import com.everlog.ui.dialog.ToastBuilder
import com.everlog.utils.ActivityUtils
import com.everlog.utils.input.KeyboardUtils
import com.facebook.shimmer.Shimmer.ColorHighlightBuilder
import com.facebook.shimmer.ShimmerFrameLayout
import rx.Observable
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity(), BaseActivityMvpView {

    protected abstract fun onActivityCreated()

    protected abstract fun getLayoutResId(): Int

    open fun getBindingView(): View? = null

    protected abstract fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>?

    protected abstract fun setupPresenter()

    protected abstract fun getAnalyticsScreenName(): String

    /**
     * Intent extras the screen can't work without. The app always passes them, so a screen opened
     * without them closes instead of crashing (see [canOpen]).
     */
    protected open fun getRequiredExtras(): List<String> = emptyList()

    /**
     * Whether the screen needs a logged in user. The app only opens these screens after login, so
     * one opened without a user closes instead of crashing (see [canOpen]).
     */
    protected open fun requiresUser(): Boolean = false

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
        enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Do NOT use activity instance state because that messes up with the ViewPager on the home screen
        super.onCreate(null)
        if (!canOpen()) {
            finish()
            return
        }
        setupBackHandling()
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

    /**
     * Called when the user navigates back. Return true if the event was consumed, otherwise the
     * default back behaviour (finishing the screen) is applied.
     *
     * Override this instead of [onBackPressed], which is not called when targeting Android 16+
     * because of predictive back.
     */
    protected open fun handleBackPressed(): Boolean {
        return getPresenter<BaseActivityMvpView>()?.onBackPressedConsumed() != false
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this) {
            if (!handleBackPressed()) {
                // Nothing consumed the event, so fall back to the default behaviour
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    // Play pre-launch robots open screens directly, without what the app would pass them. Errors are
    // recorded as non-fatals, so a bug that opens a screen this way still shows up.
    private fun canOpen(): Boolean {
        val missingExtras = getRequiredExtras().filter { intent?.extras?.get(it) == null }
        if (missingExtras.isNotEmpty()) {
            Timber.tag(javaClass.simpleName).e("Closing screen opened without required extras: %s", missingExtras)
            return false
        }
        if (requiresUser() && !LocalUserManager.hasUser()) {
            Timber.tag(javaClass.simpleName).e("Closing screen opened without a logged in user")
            return false
        }
        return true
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
        AnalyticsManager.manager.screenName(getAnalyticsScreenName())
        getPresenter<BaseActivityMvpView>()?.onActivityResumed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
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

    override fun showChoicePrompt(title: String,
                                  message: String,
                                  positive: String,
                                  negative: String,
                                  neutral: String?,
                                  destructiveButton: Int?): Observable<Int> {
        return DialogBuilder.showChoicePrompt(this, title, message, positive, negative, neutral, destructiveButton)
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