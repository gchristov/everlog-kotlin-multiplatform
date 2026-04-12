package com.everlog.ui.activities.login.resetpassword

import android.graphics.Bitmap
import android.view.View
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.databinding.ActivityResetPasswordBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.login.LoginActivity
import com.everlog.ui.activities.login.LoginActivity.Companion.setupLoadingButton
import com.everlog.utils.input.KeyboardUtils
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class ResetPasswordActivity : BaseActivity(), MvpViewResetPassword {

    private var mPresenter: PresenterResetPassword? = null
    private lateinit var binding: ActivityResetPasswordBinding

    private var mCheckBitmap: Bitmap? = null

    override fun onActivityCreated() {
        setupTopBar()
        setupButtons()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_RESET_PASSWORD
    }

    override fun onDestroy() {
        binding.resetPasswordBtn.dispose()
        super.onDestroy()
    }

    override fun onBackPressed() {
        KeyboardUtils.hideKeyboard(this)
        if (isLoading()) {
            // Wait until loading finishes
            return
        }
        super.onBackPressed()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_reset_password
    }

    override fun getBindingView(): View? {
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickResetPassword(): Observable<Void> {
        return RxView.clicks(binding.resetPasswordBtn)
    }

    override fun getResetPasswordEmail(): String {
        return binding.resetPasswordEmail.text.toString()
    }

    override fun showResetPasswordLoading(state: LoginActivity.LoadingState) {
        setButtonLoading(binding.resetPasswordBtn, state, R.color.main_accent, -1)
    }

    override fun showResetPasswordError(emailError: String) {
        binding.resetPasswordEmail.error = emailError
    }

    private fun isLoading(): Boolean {
        return binding.disableInput.visibility == View.VISIBLE
    }

    private fun setButtonLoading(button: CircularProgressButton?,
                                 state: LoginActivity.LoadingState,
                                 colorResId: Int,
                                 defaultDrawableResId: Int) {
        when (state) {
            LoginActivity.LoadingState.DEFAULT -> {
                binding.disableInput.visibility = View.GONE
                button?.revertAnimation {
                    if (defaultDrawableResId != -1) {
                        val drawable = resources.getDrawable(defaultDrawableResId)
                        button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    }
                }
            }
            LoginActivity.LoadingState.LOADING -> {
                binding.disableInput.visibility = View.VISIBLE
                button?.startAnimation()
            }
            LoginActivity.LoadingState.DONE -> {
                binding.disableInput.visibility = View.GONE
                button?.doneLoadingAnimation(ContextCompat.getColor(this, colorResId), mCheckBitmap!!)
            }
        }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterResetPassword()
    }

    private fun setupTopBar() {
        binding.appBar.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.appBar.toolbar.setNavigationOnClickListener { onBackPressed() }
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.setTitle(R.string.reset_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupButtons() {
        mCheckBitmap = setupLoadingButton()
    }
}