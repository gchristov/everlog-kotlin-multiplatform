package com.everlog.ui.activities.home.plan.create

import com.everlog.data.model.plan.ELPlan
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewCreatePlan : BaseActivityMvpView {

    fun onClickSave(): Observable<Void>

    fun onClickCover(): Observable<Void>

    fun onClickWeeks(): Observable<Void>

    fun onClickAddWeek(): Observable<Void>

    fun onNameChanged(): Observable<CharSequence>

    fun getItemToEditUuid(): String?

    fun loadPlanDetails(plan: ELPlan)

    fun getPlanName(): String

    fun togglePanel(flow: CreatePlanActivity.Flow, weekNumber: Int)
}