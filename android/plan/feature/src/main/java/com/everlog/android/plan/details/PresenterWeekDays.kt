package com.everlog.android.plan.details

import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.TemplateDetails
import com.everlog.android.plan.ui.PlanAnalytics
import com.everlog.android.plan.ui.PlanDayAdapter
import com.everlog.android.plan.ui.applyProLock
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanDay

class PresenterWeekDays(
    private val planProgress: PlanProgress,
    private val session: UserSession,
    private val analytics: PlanAnalytics,
    private val navigator: Navigator,
) : BaseFragmentPresenter<MvpViewWeekDays>() {


    private var mPlan: ELPlan? = null
    private var mWeekIndex = 0
    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<ELPlanDay>(mAdapter)

    override fun init() {
        super.init()
        setupListViews()
    }

    override fun onReady() {
        setupEditedItem()
        loadWeek()
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Loading

    private fun loadWeek() {
        mDataListManager.clear()
        val week = mPlan?.weeks?.get(mWeekIndex)
        week?.applyProLock(session.user())
        mDataListManager.addAll(week?.getDays() ?: ArrayList())
        mAdapter.notifyDataSetChanged()
    }

    // Setup

    private fun setupEditedItem() {
        mPlan = mvpView.getPlan()
        mWeekIndex = mvpView.getWeekIndex()
    }

    private fun setupListViews() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(PlanDayAdapter.Binder(analytics, navigator, session, object : PlanDayAdapter.OnPlanDayListener {
            override fun onItemClicked(item: ELPlanDay, position: Int) {
                val routine = item.getRoutine()
                if (routine != null) {
                    context?.let { navigator.open(it, TemplateDetails(routine, viewOnly = true)) }
                } else {
                    mvpView.showOK(DesignR.string.plan_details_rest_title, DesignR.string.plan_details_rest_prompt)
                }
            }

            override fun onClickChooseRoutine(day: ELPlanDay, position: Int) {}

            override fun onDayEdited() {}

            override fun onClickSkip(day: ELPlanDay) {}

            override fun onClickStart(day: ELPlanDay) {}
        }).setOngoing(planProgress.isOngoing(mvpView.getPlan())))
    }
}