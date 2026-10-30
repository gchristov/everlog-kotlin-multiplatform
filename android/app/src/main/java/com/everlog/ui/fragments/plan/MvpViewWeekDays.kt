package com.everlog.ui.fragments.plan

import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView

interface MvpViewWeekDays : BaseFragmentMvpView {

    fun getPlan(): ELPlan?

    fun getWeekIndex(): Int
}