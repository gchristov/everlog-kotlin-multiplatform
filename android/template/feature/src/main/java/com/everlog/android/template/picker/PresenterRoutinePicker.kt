package com.everlog.android.template.picker

import android.app.Activity
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.template.data.store.ELColStoreRoutinesLoadedEvent
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.template.R
import com.everlog.android.template.TemplateExtras
import com.everlog.android.template.TemplateFeature
import com.everlog.android.template.TemplateRequestCodes
import com.everlog.android.template.picker.RoutinePickerAdapter
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
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
        if (requestCode == TemplateRequestCodes.EDIT) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(TemplateExtras.ROUTINE)) {
                handlePickRoutine(Objects.requireNonNull(data.getSerializableExtra(TemplateExtras.ROUTINE)) as ELRoutine)
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
        TemplateStores.routinesStore().getItems()
    }

    // Handlers

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager?.isEmpty ?: true)
    }

    private fun handlePickRoutine(routine: ELRoutine) {
        if (routine.canBePerformed()) {
            val i = Intent()
            i.putExtra(TemplateExtras.ROUTINE, routine)
            mvpView?.setViewResult(Activity.RESULT_OK, i)
            mvpView?.closeScreen()
        } else {
            mvpView?.showOK(R.string.routines_cannot_perform_title, R.string.routines_cannot_perform_prompt)
        }
    }

    private fun handleAddRoutine() {
        mvpView.getActivity()?.let { TemplateFeature.dependencies.openTemplateEditor(it, null) }
    }

    // Setup

    private fun setupListView() {
        listAdapter = RecyclerAdapter()
        mDataListManager = DataListManager(listAdapter!!)
        listAdapter?.addDataManager(mDataListManager)
        listAdapter?.registerBinder(RoutinePickerAdapter.Binder { item: ELRoutine, _: Int -> handlePickRoutine(item) })
    }
}