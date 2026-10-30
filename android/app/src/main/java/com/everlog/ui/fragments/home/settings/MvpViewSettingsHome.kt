package com.everlog.ui.fragments.home.settings

import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.ui.dialog.AppDialogs
import rx.Observable

interface MvpViewSettingsHome : BaseFragmentMvpView {

    fun showAppBlockerPrompt(type: AppDialogs.AppBlockerDialogType): Observable<Int>

    fun onClickMuscleGoal(): Observable<Void>

    fun onClickWeightIncrease(): Observable<Void>

    fun onClickWeeklyGoal(): Observable<Void>

    fun onClickGooglePlay(): Observable<Void>

    fun onClickLogout(): Observable<Void>

    fun onClickFeedback(): Observable<Void>

    fun onClickWeightUnit(): Observable<Void>

    fun onClickFirstWeekDay(): Observable<Void>

    fun onClickShare(): Observable<Void>

    fun onClickManagePro(): Observable<Void>

    fun onClickDesignSystem(): Observable<Void>

    // Integrations

    fun onClickManageIntegrationGoogleFit(): Observable<Void>

    // Consent

    fun onCheckChangeNewsletter(): Observable<Boolean>

    fun onCheckChangeKeepScreenOn(): Observable<Boolean>

    fun showAppInfo(appInfo: String)

    fun showFeedback(visible: Boolean)

    fun showUserInfo(user: ELUser)

    fun showSettings(viewModel: SettingsHomeFragment.SettingsViewModel)

    fun showPickerWeight(value: Float): Observable<Float>

    fun showPickerNumber(value: Int, type: AppDialogs.NumberPickerDialogType): Observable<Int>

    fun showPickerMultipleChoice(titleResId: Int, values: Array<String>, selectedIndex: Int): Observable<Int>
}