package com.everlog.ui.activities.home

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewHome : BaseActivityMvpView {

    fun onClickAddFab(): Observable<Void>

    fun onClickAddWeekEmptyState(): Observable<Void>

    fun showWeek()

    fun toggleStartWorkoutButton(visible: Boolean)

    fun appUpdateLauncher(): ActivityResultLauncher<IntentSenderRequest>

    fun onAppUpdateFlowResult(): Observable<Int>

    fun onClickAppUpdateRestart(): Observable<Void>

    fun showAppUpdateReady()
}