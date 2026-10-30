package com.everlog.ui.fragments.plan

import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanDay
import com.everlog.managers.PlanManager
import com.everlog.ui.adapters.plan.PlanDayAdapter
import com.everlog.ui.adapters.plan.applyProLock
import com.everlog.ui.fragments.base.BaseFragmentPresenter

class PresenterWeekDays : BaseFragmentPresenter<MvpViewWeekDays>() {

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
        week?.applyProLock()
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
        mAdapter.registerBinder(PlanDayAdapter.Binder(object : PlanDayAdapter.OnPlanDayListener {
            override fun onItemClicked(item: ELPlanDay, position: Int) {
                if (item.getRoutine() != null) {
                    navigator.openRoutineDetails(item.getRoutine(), true)
                } else {
                    mvpView.showOK(R.string.plan_details_rest_title, R.string.plan_details_rest_prompt)
                }
            }

            override fun onClickChooseRoutine(day: ELPlanDay, position: Int) {}

            override fun onDayEdited() {}

            override fun onClickSkip(day: ELPlanDay) {}

            override fun onClickStart(day: ELPlanDay) {}
        }).setOngoing(PlanManager.manager.isOngoing(mvpView.getPlan())))
    }
}