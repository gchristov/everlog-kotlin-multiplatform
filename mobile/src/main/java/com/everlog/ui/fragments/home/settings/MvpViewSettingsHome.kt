package com.everlog.ui.fragments.home.settings

import com.everlog.data.model.ELUser
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import rx.Observable

interface MvpViewSettingsHome : BaseFragmentMvpView {

    fun onClickMuscleGoal(): Observable<Void>

    fun onClickWeightIncrease(): Observable<Void>

    fun onClickWeeklyGoal(): Observable<Void>

    fun onClickGooglePlay(): Observable<Void>

    fun onClickFacebook(): Observable<Void>

    fun onClickTwitter(): Observable<Void>

    fun onClickLogout(): Observable<Void>

    fun onClickReportProblem(): Observable<Void>

    fun onClickWeightUnit(): Observable<Void>

    fun onClickFirstWeekDay(): Observable<Void>

    fun onClickShare(): Observable<Void>

    fun onClickManagePro(): Observable<Void>

    // Integrations

    fun onClickManageIntegrationGoogleFit(): Observable<Void>

    // Consent

    fun onCheckChangeNewsletter(): Observable<Boolean>

    fun showAppInfo(appInfo: String)

    fun showUserInfo(user: ELUser)

    fun showSettings(viewModel: SettingsHomeFragment.SettingsViewModel)

    fun showPickerWeight(value: Float): Observable<Float>

    fun showPickerNumber(value: Int, type: DialogBuilder.NumberPickerDialogType): Observable<Int>

    fun showPickerMultipleChoice(values: Array<String>, selectedIndex: Int, type: DialogBuilder.MultipleChoiceDialogType): Observable<Int>
}