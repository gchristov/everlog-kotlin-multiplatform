package com.everlog.ui.activities.home.plan.details

import com.everlog.data.model.plan.ELPlan
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewPlanDetails : BaseActivityMvpView {

    fun onClickToggleStartPlan(): Observable<Void>

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun getItemToEditUuid(): String?

    fun loadPlanDetails(plan: ELPlan, showStop: Boolean)
}