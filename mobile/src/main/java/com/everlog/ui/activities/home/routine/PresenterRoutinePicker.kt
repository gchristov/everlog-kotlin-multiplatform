package com.everlog.ui.activities.home.routine

import android.app.Activity
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.routines.ELUserRoutinesStore.ELColStoreRoutinesLoadedEvent
import com.everlog.data.model.ELRoutine
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.adapters.routine.RoutinePickerAdapter
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import java.util.*

class PresenterRoutinePicker : BaseActivityPresenter<MvpViewRoutinePicker>() {

    private var listAdapter: RecyclerAdapter? = null
    private var mDataListManager: DataListManager<ELRoutine>? = null

    override fun init() {
        super.init()
        setupListView()
    }

    override fun onReady() {
        observeAddRoutineClick()
        loadRoutines()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_ROUTINE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ELConstants.EXTRA_ROUTINE)) {
                handlePickRoutine(Objects.requireNonNull(data.getSerializableExtra(ELConstants.EXTRA_ROUTINE)) as ELRoutine)
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun shouldCloseOnWorkoutStart(): Boolean {
        return true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRoutinesLoaded(event: ELColStoreRoutinesLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView?.toggleLoadingOverlay(false)
                handleCheckEmptyState()
                handleError(event.error)
            } else {
                mDataListManager?.clear()
                mDataListManager?.addAll(event.items)
                if (!event.isFromCache) {
                    mvpView?.toggleLoadingOverlay(false)
                    listAdapter?.notifyDataSetChanged()
                    handleCheckEmptyState()
                } else if (mDataListManager?.isEmpty == false) {
                    mvpView?.toggleLoadingOverlay(false)
                    handleCheckEmptyState()
                }
            }
        }
    }

    internal fun getListAdapter(): RecyclerAdapter? {
        return listAdapter
    }

    // Observers

    private fun observeAddRoutineClick() {
        subscriptions.add(Observable.merge(mvpView.onClickAddRoutine(), mvpView.onClickEmptyAction())
                .compose(applyUISchedulers())
                .subscribe { handleAddRoutine() })
    }

    // Loading

    private fun loadRoutines() {
        if (mDataListManager?.isEmpty == true) {
            mvpView?.toggleLoadingOverlay(true)
        }
        // Load initial items
        ELDatastore.routinesStore().getItems()
    }

    // Handlers

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager?.isEmpty ?: true)
    }

    private fun handlePickRoutine(routine: ELRoutine) {
        if (routine.canBePerformed()) {
            val i = Intent()
            i.putExtra(ELConstants.EXTRA_ROUTINE, routine)
            mvpView?.setViewResult(Activity.RESULT_OK, i)
            mvpView?.closeScreen()
        } else {
            mvpView?.showOK(R.string.routines_cannot_perform_title, R.string.routines_cannot_perform_prompt)
        }
    }

    private fun handleAddRoutine() {
        navigator.openEditRoutine(CreateRoutineActivity.Companion.Properties()
                .showDetailsOnSuccess(false))
    }

    // Setup

    private fun setupListView() {
        listAdapter = RecyclerAdapter()
        mDataListManager = DataListManager(listAdapter!!)
        listAdapter?.addDataManager(mDataListManager)
        listAdapter?.registerBinder(RoutinePickerAdapter.Binder { item: ELRoutine, _: Int -> handlePickRoutine(item) })
    }
}