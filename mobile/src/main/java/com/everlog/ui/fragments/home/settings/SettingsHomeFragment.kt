package com.everlog.ui.fragments.home.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.data.model.ELIntegration
import com.everlog.data.model.ELUser
import com.everlog.databinding.FragmentHomeSettingsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.ui.views.CheckBoxTriStates
import com.everlog.utils.ViewUtils
import com.everlog.utils.format.FormatUtils
import com.everlog.utils.glide.ELGlideModule
import com.jakewharton.rxbinding.view.RxView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import rx.Observable
import java.util.*

class SettingsHomeFragment : BaseTabFragment(), MvpViewSettingsHome {

    private var mPresenter: PresenterSettingsHome? = null
    private var _binding: FragmentHomeSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        setupButtons()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_SETTINGS
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_settings
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHomeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun onClickMuscleGoal(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.muscleGoalBtn))
    }

    override fun onClickWeightIncrease(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.weightIncreaseBtn))
    }

    override fun onClickWeeklyGoal(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.weeklyGoalBtn))
    }

    override fun onClickGooglePlay(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.playStoreBtn))
    }

    override fun onClickFacebook(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.facebookBtn))
    }

    override fun onClickTwitter(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.twitterBtn))
    }

    override fun onClickLogout(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.logoutBtn))
    }

    override fun onClickReportProblem(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.reportProblemBtn))
    }

    override fun onClickWeightUnit(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.weightUnitBtn))
    }

    override fun onClickFirstWeekDay(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.firstWeekDayBtn))
    }

    override fun onClickShare(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.shareBtn))
    }

    override fun onClickManagePro(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.manageProBtn))
    }

    override fun onClickManageIntegrationGoogleFit(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.googleFitBtn))
    }

    override fun onCheckChangeNewsletter(): Observable<Boolean> {
        return binding.root.findViewById<CheckBoxTriStates>(R.id.newsletterCheckbox).observeCheckChange()
    }

    override fun showAppInfo(appInfo: String) {
        binding.root.findViewById<TextView>(R.id.versionLbl).text = appInfo
    }

    override fun showSettings(viewModel: SettingsViewModel) {
        binding.root.findViewById<TextView>(R.id.muscleGoalField).text = viewModel.muscleGoal?.valueName(activity)
        binding.root.findViewById<TextView>(R.id.muscleGoalSubtitle).text = viewModel.muscleGoal?.valueSettingsSummary(activity, false)
        binding.root.findViewById<TextView>(R.id.weightIncreaseField).text = String.format("%s %s", FormatUtils.formatSetWeight(viewModel.weightIncrease!!), SettingsManager.weightUnitAbbreviation())
        binding.root.findViewById<TextView>(R.id.weeklyGoalField).text = String.format("%d", viewModel.weeklyGoals)
        // Units
        binding.root.findViewById<TextView>(R.id.weightUnitField).text = getString(if (viewModel.weightUnit == SettingsManager.WeightUnit.KILOGRAM) R.string.settings_kilograms else R.string.settings_pounds)
        // First week day
        binding.root.findViewById<TextView>(R.id.firstWeekDayField).text = viewModel.firstWeekDay?.getDisplayName(TextStyle.FULL, Locale.getDefault())
        // Integrations
        binding.root.findViewById<View>(R.id.integrationsSection).visibility = if (viewModel.integrationGoogleFitEnabled == true) View.VISIBLE else View.GONE
        ELIntegration.Type.GOOGLE_FIT.getIcon()?.let { binding.root.findViewById<ImageView>(R.id.googleFitImg).setImageResource(it) }
        binding.root.findViewById<TextView>(R.id.googleFitTitle).text = context?.let { ELIntegration.Type.GOOGLE_FIT.getTitle() }
        binding.root.findViewById<TextView>(R.id.googleFitField).text = getString(if (viewModel.integrationGoogleFitEnabled == true) R.string.integrations_connected else R.string.integrations_connected_prompt)
        // Notifications
        binding.root.findViewById<CheckBoxTriStates>(R.id.newsletterCheckbox).setChecked(viewModel.notificationNewsletterEnabled ?: false)
    }

    override fun showUserInfo(user: ELUser) {
        // User info
        binding.root.findViewById<TextView>(R.id.userNameLbl).text = user.getFirstName()
        if (user.photoUrl != null) {
            binding.root.findViewById<View>(R.id.userAvatar).setPadding(0, 0, 0, 0)
            ELGlideModule.loadImage(user.photoUrl, binding.root.findViewById(R.id.userAvatar))
        } else {
            val p = ViewUtils.dpToPxFromRaw(requireContext(), R.dimen.activity_margin)
            binding.root.findViewById<View>(R.id.userAvatar).setPadding(p, p, p, p)
        }
        // Pro
        // Todo: Edited as per #181
        // Show Pro only to paying users
        val paying = user.subscription?.isPro() == true
        binding.root.findViewById<View>(R.id.proBadge).visibility = if (paying) View.VISIBLE else View.GONE
        binding.root.findViewById<View>(R.id.manageProBtn).visibility = if (paying) View.VISIBLE else View.GONE
        binding.root.findViewById<Button>(R.id.manageProBtn).setTextColor(ContextCompat.getColor(requireContext(), if (paying) R.color.main_accent else R.color.background_card))
        binding.root.findViewById<Button>(R.id.manageProBtn).setBackgroundResource(if (paying) R.drawable.rounded_corners_btn_three else R.drawable.rounded_corners_btn_one)
        binding.root.findViewById<Button>(R.id.manageProBtn).setText(if (paying) R.string.settings_pro_manage else R.string.settings_pro_upgrade)
        val trialDaysLeft = user.proFreeTrialDaysRemaining()
        binding.root.findViewById<View>(R.id.proFreeTrialSummary).visibility = if (user.isProWithinFreeTrial()) View.VISIBLE else View.GONE
        binding.root.findViewById<TextView>(R.id.proFreeTrialSummary).text = if (trialDaysLeft == 0) getString(R.string.settings_pro_free_trial_today) else resources.getQuantityString(R.plurals.settings_pro_free_trial, trialDaysLeft, trialDaysLeft)
    }

    override fun showPickerWeight(value: Float): Observable<Float> {
        return DialogBuilder.showWeightIncreaseDialog(context, value)
    }

    override fun showPickerNumber(value: Int, type: DialogBuilder.NumberPickerDialogType): Observable<Int> {
        return DialogBuilder.showPickerNumberDialog(context, value, type)
    }

    override fun showPickerMultipleChoice(values: Array<String>, selectedIndex: Int, type: DialogBuilder.MultipleChoiceDialogType): Observable<Int> {
        return DialogBuilder.showPickerMultipleChoiceDialog(context, values, selectedIndex, type)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterSettingsHome()
    }

    private fun setupButtons() {
        binding.root.findViewById<View>(R.id.newsletterBtn).setOnClickListener {
            binding.root.findViewById<View>(R.id.newsletterCheckbox).performClick()
        }
    }

    class SettingsViewModel {

        var muscleGoal: SettingsManager.MuscleGoal? = null
            private set
        var weightIncrease: Float? = null
            private set
        var weeklyGoals: Int? = null
            private set
        var weightUnit: SettingsManager.WeightUnit? = null
            private set
        var firstWeekDay: DayOfWeek? = null
            private set
        var integrationGoogleFitEnabled: Boolean? = null
            private set
        var notificationNewsletterEnabled: Boolean? = null
            private set

        fun muscleGoal(muscleGoal: SettingsManager.MuscleGoal) = apply { this.muscleGoal = muscleGoal }
        fun weightIncrease(weightIncrease: Float) = apply { this.weightIncrease = weightIncrease }
        fun weeklyGoals(weeklyGoals: Int) = apply { this.weeklyGoals = weeklyGoals }
        fun weightUnit(weightUnit: SettingsManager.WeightUnit) = apply { this.weightUnit = weightUnit }
        fun firstWeekDay(firstWeekDay: DayOfWeek) = apply { this.firstWeekDay = firstWeekDay }
        fun integrationGoogleFitEnabled(integrationGoogleFitEnabled: Boolean) = apply { this.integrationGoogleFitEnabled = integrationGoogleFitEnabled }
        fun notificationNewsletterEnabled(notificationNewsletterEnabled: Boolean?) = apply { this.notificationNewsletterEnabled = notificationNewsletterEnabled }
    }
}