package com.everlog.android.plan.details

import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewPlanDetails : BaseActivityMvpView {

    fun onClickToggleStartPlan(): Observable<Void>

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun getItemToEditUuid(): String?

    fun loadPlanDetails(plan: ELPlan, showStop: Boolean)
}