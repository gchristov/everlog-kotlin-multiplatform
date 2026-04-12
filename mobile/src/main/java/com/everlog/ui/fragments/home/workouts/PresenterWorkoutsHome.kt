package com.everlog.ui.fragments.home.workouts

import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.plans.ELUserPlansStore
import com.everlog.data.datastores.routines.ELUserRoutinesStore.ELColStoreRoutinesLoadedEvent
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.util.Creator
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.adapters.CreatorAdapter
import com.everlog.ui.adapters.plan.PlanAdapter
import com.everlog.ui.adapters.routine.RoutineAdapter
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.utils.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterWorkoutsHome : BaseFragmentPresenter<MvpViewWorkoutsHome>() {

    // Plans

    private var mDelayAddPlan = false
    private val mAdapterPlans = RecyclerAdapter()
    private val mDataListManagerPlans = DataListManager<Any>(mAdapterPlans)

    // Routines

    private val mAdapterRoutines = RecyclerAdapter()
    private val mDataListManagerRoutines = DataListManager<Any>(mAdapterRoutines)

    override fun init() {
        super.init()
        setupListViews()
    }

    override fun onReady() {
        observeExercisesClick()
        // Plans
        observeAddPlanClick()
        loadPlans()
        // Routines
        observeAddRoutineClick()
        loadRoutines()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansLoaded(event: ELUserPlansStore.ELColStorePlansLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView?.toggleLoadingOverlayPlans(false)
                handleError(event.error)
            } else {
                val delay = if (mDelayAddPlan) 500 else 0
                Utils.runWithDelay({
                    mDataListManagerPlans.clear()
                    if (event.items.isEmpty()) {
                        mDataListManagerPlans.add(Creator())
                    }
                    mDataListManagerPlans.addAll(event.items)
                    if (!event.isFromCache) {
                        mvpView?.toggleLoadingOverlayPlans(false)
                        mAdapterPlans.notifyDataSetChanged()
                    } else if (!mDataListManagerPlans.isEmpty) {
                        mvpView?.toggleLoadingOverlayPlans(false)
                    }
                }, delay)
                mDelayAddPlan = false
            }
            mvpView?.togglePlanCreateTop(event.items.isNotEmpty())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRoutinesLoaded(event: ELColStoreRoutinesLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView.toggleLoadingOverlayRoutines(false)
                handleError(event.error)
            } else {
                mDataListManagerRoutines.clear()
                if (event.items.isEmpty()) {
                    mDataListManagerRoutines.add(Creator())
                }
                mDataListManagerRoutines.addAll(event.items)
                if (!event.isFromCache) {
                    mvpView?.toggleLoadingOverlayRoutines(false)
                    mAdapterRoutines.notifyDataSetChanged()
                } else if (!mDataListManagerRoutines.isEmpty) {
                    mvpView.toggleLoadingOverlayRoutines(false)
                }
            }
        }
        mvpView?.toggleRoutineCreateTop(event.items.isNotEmpty())
    }

    internal fun getPlansAdapter(): RecyclerAdapter? {
        return mAdapterPlans
    }

    internal fun getRoutinesAdapter(): RecyclerAdapter? {
        return mAdapterRoutines
    }

    // Observers

    private fun observeExercisesClick() {
        subscriptions.add(mvpView.onClickExercises()
                .compose(applyUISchedulers())
                .subscribe {
                    navigator.openExercises()
                })
    }

    private fun observeAddPlanClick() {
        subscriptions.add(mvpView.onClickAddPlanTop()
                .compose(applyUISchedulers())
                .subscribe {
                    handleAddPlan()
                })
    }

    private fun observeAddRoutineClick() {
        subscriptions.add(mvpView.onClickAddRoutineTop()
                .compose(applyUISchedulers())
                .subscribe {
                    handleAddRoutine()
                })
    }

    // Loading

    private fun loadPlans() {
        if (mDataListManagerPlans.isEmpty) {
            mvpView?.toggleLoadingOverlayPlans(true)
        }
        // Load initial items.
        ELDatastore.plansStore().getItems()
    }

    private fun loadRoutines() {
        if (mDataListManagerRoutines.isEmpty) {
            mvpView?.toggleLoadingOverlayRoutines(true)
        }
        // Load initial items.
        ELDatastore.routinesStore().getItems()
    }

    // Handlers

    private fun handleAddPlan() {
        mDelayAddPlan = true
        navigator.openEditPlan(null)
    }

    private fun handleAddRoutine() {
        navigator.openEditRoutine(CreateRoutineActivity.Companion.Properties()
                .showDetailsOnSuccess(true))
    }

    // Setup

    private fun setupListViews() {
        // Plans
        mAdapterPlans.addDataManager(mDataListManagerPlans)
        val plansBinder = PlanAdapter.Binder(object : PlanAdapter.OnPlanListener {
            override fun onItemClicked(item: ELPlan, position: Int) {
                navigator.openPlanDetails(item)
            }
        })
        val createPlanBinder = CreatorAdapter.Binder(R.layout.row_plan_create) { _, _ -> handleAddPlan() }
        mAdapterPlans.registerBinders(plansBinder, createPlanBinder)
        // Routines
        mAdapterRoutines.addDataManager(mDataListManagerRoutines)
        val routinesBinder = RoutineAdapter.Binder { item, _ -> navigator.openRoutineDetails(item, false) }
        val createRoutineBinder = CreatorAdapter.Binder(R.layout.row_routine_create) { _, _ -> handleAddRoutine() }
        mAdapterRoutines.registerBinders(routinesBinder, createRoutineBinder)
    }
}