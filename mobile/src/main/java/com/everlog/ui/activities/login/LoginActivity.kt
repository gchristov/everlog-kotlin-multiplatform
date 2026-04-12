package com.everlog.ui.activities.login

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.view.View
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.databinding.ActivityLoginBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.fragments.base.BaseFragment
import com.everlog.ui.fragments.onboarding.OnboardingPageFragment
import com.everlog.ui.views.CheckBoxTriStates
import com.everlog.ui.views.viewpager.ELFragmentPagerAdapter
import com.everlog.utils.ActivityUtils
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import com.everlog.utils.text.TextViewUtils
import com.everlog.utils.text.TouchableSpan
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject

class LoginActivity : BaseActivity(), MvpViewLogin {

    companion object {
        fun setupLoadingButton(): Bitmap {
            val d = ELApplication.getInstance().resources.getDrawable(R.drawable.ic_check_green)
            d.setColorFilter(ContextCompat.getColor(ELApplication.getInstance(), R.color.background_base), PorterDuff.Mode.SRC_ATOP)
            val bitmap = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            d.setBounds(0, 0, canvas.width, canvas.height)
            d.draw(canvas)
            return bitmap
        }

    }

    enum class FormType {
        INTRO, LOGIN, REGISTER
    }

    enum class LoadingState {
        DEFAULT, LOADING, DONE
    }

    private var mPresenter: PresenterLogin? = null
    private lateinit var binding: ActivityLoginBinding

    private var mCheckBitmap: Bitmap? = null

    private val mOnClickTerms = PublishSubject.create<Void>()
    private val mOnClickPrivacy = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        // APP STARTUP: Delay to not block
        Utils.runWithDelay({
            setupTopBar()
            setupBackgroundImage()
            setupUnderlineTexts()
            setupPager()
            setupButtons()
            setupFormNavigation()
            showForm(FormType.INTRO)
        }, 10)
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_LOGIN
    }

    override fun onDestroy() {
        binding.formLogin.loginBtn.dispose()
        binding.formRegister.registerBtn.dispose()
        super.onDestroy()
    }

    override fun onBackPressed() {
        KeyboardUtils.hideKeyboard(this)
        if (isLoading()) {
            // Wait until loading finishes
            return
        }
        if (binding.formIntro.root.visibility != View.VISIBLE) {
            showForm(FormType.INTRO)
        } else {
            super.onBackPressed()
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_login
    }

    override fun getBindingView(): View? {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickLogin(): Observable<Void> {
        return RxView.clicks(binding.formLogin.loginBtn)
    }

    override fun onClickLoginGoogle(): Observable<Void> {
        return RxView.clicks(binding.formLogin.loginGoogleBtn)
    }

    override fun onClickRegister(): Observable<Void> {
        return RxView.clicks(binding.formRegister.registerBtn)
    }

    override fun onClickRegisterGoogle(): Observable<Void> {
        return RxView.clicks(binding.formRegister.registerGoogleBtn)
    }

    override fun onClickTerms(): Observable<Void> {
        return mOnClickTerms
    }

    override fun onClickResetPassword(): Observable<Void> {
        return RxView.clicks(binding.formLogin.resetPasswordBtn)
    }

    override fun onClickPrivacy(): Observable<Void> {
        return mOnClickPrivacy
    }

    override fun getLoginEmail(): String {
        return binding.formLogin.loginEmail.text.toString()
    }

    override fun getLoginPassword(): String {
        return binding.formLogin.loginPassword.text.toString()
    }

    override fun getRegisterFullName(): String {
        return binding.formRegister.registerName.text.toString()
    }

    override fun getRegisterEmail(): String {
        return binding.formRegister.registerEmail.text.toString()
    }

    override fun getRegisterPassword(): String {
        return binding.formRegister.registerPassword.text.toString()
    }

    override fun termsAccepted(): Boolean {
        return binding.formRegister.termsCheckbox.isChecked
    }

    override fun newsletterDecided(): Boolean {
        return binding.formRegister.newsletterCheckbox.getState() != CheckBoxTriStates.State.UNKNOWN
    }

    override fun newsletterAccepted(): Boolean {
        return binding.formRegister.newsletterCheckbox.getState() == CheckBoxTriStates.State.CHECKED
    }

    override fun showGoogleLoading(state: LoadingState) {
        if (binding.formLogin.root.visibility == View.VISIBLE) {
            setButtonLoading(binding.formLogin.loginGoogleBtn, state, R.color.white_base, com.google.android.gms.base.R.drawable.googleg_disabled_color_18)
        } else {
            setButtonLoading(binding.formRegister.registerGoogleBtn, state, R.color.white_base, com.google.android.gms.base.R.drawable.googleg_disabled_color_18)
        }
    }

    override fun showLoginLoading(state: LoadingState) {
        setButtonLoading(binding.formLogin.loginBtn, state, R.color.main_accent, -1)
    }

    override fun showRegisterLoading(state: LoadingState) {
        setButtonLoading(binding.formRegister.registerBtn, state, R.color.main_accent, -1)
    }

    override fun showLoginError(emailError: String?, passwordError: String?) {
        binding.formLogin.loginEmail.error = emailError
        binding.formLogin.loginPassword.error = passwordError
    }

    override fun showRegisterError(nameError: String?,
                                   emailError: String?,
                                   passwordError: String?) {
        binding.formRegister.registerName.error = nameError
        binding.formRegister.registerEmail.error = emailError
        binding.formRegister.registerPassword.error = passwordError
    }

    private fun showForm(type: FormType) {
        when (type) {
            FormType.INTRO -> {
                binding.toolbar.visibility = View.GONE
                binding.formIntro.root.visibility = View.VISIBLE
                binding.formLogin.root.visibility = View.GONE
                binding.formRegister.root.visibility = View.GONE
            }
            FormType.LOGIN -> {
                binding.toolbar.setTitle(R.string.login_sign_in)
                binding.toolbar.visibility = View.VISIBLE
                binding.formIntro.root.visibility = View.GONE
                binding.formLogin.root.visibility = View.VISIBLE
                binding.formRegister.root.visibility = View.GONE
            }
            FormType.REGISTER -> {
                binding.toolbar.setTitle(R.string.login_register)
                binding.toolbar.visibility = View.VISIBLE
                binding.formIntro.root.visibility = View.GONE
                binding.formLogin.root.visibility = View.GONE
                binding.formRegister.root.visibility = View.VISIBLE
            }
        }
    }

    private fun isLoading(): Boolean {
        return binding.disableInput.visibility == View.VISIBLE
    }

    private fun setButtonLoading(button: CircularProgressButton?,
                                 state: LoadingState,
                                 colorResId: Int,
                                 defaultDrawableResId: Int) {
        when (state) {
            LoadingState.DEFAULT -> {
                binding.disableInput.visibility = View.GONE
                button?.revertAnimation {
                    if (defaultDrawableResId != -1) {
                        val drawable = resources.getDrawable(defaultDrawableResId)
                        button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    }
                }
            }
            LoadingState.LOADING -> {
                binding.disableInput.visibility = View.VISIBLE
                button?.startAnimation()
            }
            LoadingState.DONE -> {
                binding.disableInput.visibility = View.GONE
                button?.doneLoadingAnimation(ContextCompat.getColor(this, colorResId), mCheckBitmap!!)
            }
        }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterLogin()
    }

    private fun setupTopBar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupBackgroundImage() {
        ActivityUtils.setupBackgroundImage(this, R.drawable.background_login, R.id.backgroundView)
    }

    private fun setupUnderlineTexts() {
        // Terms and privacy
        val termsSpan = object : TouchableSpan(binding.formRegister.termsLbl.currentTextColor) {
            override fun onClick(widget: View) {
                mOnClickTerms.onNext(null)
            }
        }
        val privacySpan = object : TouchableSpan(binding.formRegister.termsLbl.currentTextColor) {
            override fun onClick(widget: View) {
                mOnClickPrivacy.onNext(null)
            }
        }
        TextViewUtils.addClickableSpans(binding.formRegister.termsLbl, arrayOf(termsSpan, privacySpan), getString(R.string.login_terms_full), arrayOf(getString(R.string.login_terms), getString(R.string.login_terms_privacy)))
    }

    private fun setupPager() {
        val fragments = ArrayList<BaseFragment>()
        fragments.add(OnboardingPageFragment(R.string.login_onboard_workout_title, R.string.login_onboard_workout_subtitle, R.drawable.background_onboard_login_workout))
        fragments.add(OnboardingPageFragment(R.string.login_onboard_activity_title, R.string.login_onboard_activity_subtitle, R.drawable.background_onboard_login_activity))
        fragments.add(OnboardingPageFragment(R.string.login_onboard_routine_title, R.string.login_onboard_routine_subtitle, R.drawable.background_onboard_login_routine))
        fragments.add(OnboardingPageFragment(R.string.login_onboard_plans_title, R.string.login_onboard_plans_subtitle, R.drawable.background_onboard_login_plans))
        fragments.add(OnboardingPageFragment(R.string.login_onboard_home_plan_title, R.string.login_onboard_home_plan_subtitle, R.drawable.background_onboard_login_home_plan))
        val adapter = ELFragmentPagerAdapter(supportFragmentManager)
        binding.formIntro.pager.offscreenPageLimit = fragments.size
        binding.formIntro.pager.adapter = adapter
        adapter.setItems(fragments)
        binding.formIntro.indicator.attachToPager(binding.formIntro.pager)
    }

    private fun setupFormNavigation() {
        binding.formIntro.showLoginBtn.setOnClickListener {
            showForm(FormType.LOGIN)
        }
        binding.formIntro.showRegisterBtn.setOnClickListener {
            showForm(FormType.REGISTER)
        }
    }

    private fun setupButtons() {
        mCheckBitmap = setupLoadingButton()
    }
}