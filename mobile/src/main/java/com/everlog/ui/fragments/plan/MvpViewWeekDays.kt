package com.everlog.ui.fragments.plan

import com.everlog.data.model.plan.ELPlan
import com.everlog.ui.fragments.base.BaseFragmentMvpView

interface MvpViewWeekDays : BaseFragmentMvpView {

    fun getPlan(): ELPlan?

    fun getWeekIndex(): Int
}