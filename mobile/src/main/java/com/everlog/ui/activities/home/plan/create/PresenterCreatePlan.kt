package com.everlog.ui.activities.home.plan.create

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELActivityRequestCodes.REQUEST_PICK_COVER_IMAGE
import com.everlog.constants.ELConstants
import com.everlog.constants.ELConstants.EXTRA_ROUTINE
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.plans.ELUserPlanStore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanDay
import com.everlog.data.model.plan.ELPlanWeek
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.adapters.plan.PlanDayAdapter
import com.everlog.ui.adapters.plan.PlanWeekAdapter
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import com.google.firebase.firestore.SetOptions
import com.imagepick.picker.dialog.ELPickerDialog
// import icepick.State
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterCreatePlan : BaseActivityPresenter<MvpViewCreatePlan>() {

    // @State
    @JvmField
    var mChangesMade = false
    // @State
    @JvmField
    var mShowingPanel = CreatePlanActivity.Flow.INFO
    // @State
    @JvmField
    var toEdit: ELPlan? = null
    // @State
    @JvmField
    var mSelectedWeek: Int? = null
    // @State
    @JvmField
    var mSelectedDay: Int? = null
    // @State
    @JvmField
    var mInitialDataSet = false
    // @State
    @JvmField
    var mLoadedItemOnce = false
    private var mEditMode = false
    private var mSavePressed = false

    // Weeks

    private val mWeeksAdapter = RecyclerAdapter()
    private val mWeeksDataListManager = DataListManager<ELPlanWeek>(mWeeksAdapter)

    // Days

    private val mDaysAdapter = RecyclerAdapter()
    private val mDaysDataListManager = DataListManager<ELPlanDay>(mDaysAdapter)

    override fun init() {
        super.init()
        setupListViews()
    }

    override fun onReady() {
        setupEditedItem()
        setupState()
        observeDynamicChanges()
        observeSaveClick()
        observeCoverClick()
        observeWeeksClick()
        observeAddWeekClick()
        loadPlan()
    }

    override fun onBackPressedConsumed(): Boolean {
        if (!attemptBack()) {
            if (mChangesMade) {
                observeDiscard()
            } else {
                checkShouldDiscard()
            }
        }
        return true
    }

    override fun detachView() {
        ELDatastore.planStore().destroy()
        super.detachView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_COVER_IMAGE) {
            if (resultCode == RESULT_OK) {
                val url = data?.getStringExtra(ELConstants.EXTRA_COVER_IMAGE)
                handleImagePicked(url)
            }
            return
        } else if (requestCode == ELActivityRequestCodes.REQUEST_PICK_ROUTINE) {
            if (resultCode == RESULT_OK) {
                handleRoutinePicked(data?.getSerializableExtra(EXTRA_ROUTINE) as? ELRoutine)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlanLoaded(event: ELUserPlanStore.ELDocStorePlanLoadedEvent) {
        if (isAttachedToView) {
            mvpView?.toggleLoadingOverlay(false)
            if (event.error != null) {
                handleError(event.error)
            } else {
                if (!mLoadedItemOnce || event.isHasPendingWrites) {
                    toEdit = event.item
                    if (PlanManager.manager.isOngoing(toEdit)) {
                        toEdit = PlanManager.manager.ongoingPlan()
                    }
                }
                loadViewData()
                mLoadedItemOnce = true
            }
        }
    }

    internal fun getWeeksListAdapter(): RecyclerAdapter? {
        return mWeeksAdapter
    }

    internal fun getDaysListAdapter(): RecyclerAdapter? {
        return mDaysAdapter
    }

    // Observers

    private fun observeAddWeekClick() {
        subscriptions.add(mvpView.onClickAddWeek()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        handleAddWeek()
                    }
                })
    }

    private fun observeWeeksClick() {
        subscriptions.add(mvpView.onClickWeeks()
                .compose(applyUISchedulers())
                .subscribe {
                    KeyboardUtils.hideKeyboard(mvpView?.getActivity())
                    if (toEdit != null) {
                        if (canShowWeeksList()) {
                            showWeeks()
                        } else {
                            val week = toEdit?.weeks?.first()
                            showDays(week, 0)
                        }
                    }
                })
    }

    private fun canShowWeeksList(): Boolean {
        return toEdit?.hasWeeksWithWorkouts() == true || toEdit?.weeks?.size!! > 1
    }

    private fun observeCoverClick() {
        subscriptions.add(mvpView.onClickCover()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        if (toEdit?.imageUrl != null) {
                            handleChangeImage()
                        } else {
                            navigator.openCoverImagePicker()
                        }
                    }
                })
    }

    private fun observeDynamicChanges() {
        subscriptions.add(mvpView.onNameChanged()
                .filter { text -> text.toString() != toEdit?.name ?: "" }
                .compose(applyUISchedulers())
                .subscribe({ changesMade() }, { throwable -> handleError(throwable) }))
    }

    private fun observeDiscard() {
        subscriptions.add(mvpView.showPrompt(R.string.discard_title, R.string.discard_prompt, R.string.discard, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        checkShouldDiscard()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeSaveClick() {
        subscriptions.add(mvpView.onClickSave()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        if (!attemptBack()) {
                            handleSavePlan()
                        }
                    }
                })
    }

    // Loading

    private fun loadPlan() {
        if (mEditMode) {
            mvpView?.toggleLoadingOverlay(true)
            // Listen for item changes
            ELDatastore.planStore().getItem(getPlanUuid())
        } else {
            // New plan is already created in firebase, so refresh UI
            mvpView?.toggleLoadingOverlay(false)
            loadViewData()
        }
    }

    private fun loadViewData() {
        mvpView?.loadPlanDetails(toEdit!!)
        Utils.runWithDelay({ mInitialDataSet = true }, 300)
    }

    private fun refreshInfo() {
        changesMade()
        loadViewData()
    }

    private fun refreshLists() {
        mWeeksAdapter.notifyDataSetChanged()
        mDaysAdapter.notifyDataSetChanged()
    }

    private fun getPlanUuid(): String {
        val uuid = mvpView.getItemToEditUuid()
        if (uuid != null) {
            return uuid
        }
        return toEdit!!.uuid!!
    }

    // Handlers

    private fun handleSavePlan() {
        KeyboardUtils.hideKeyboard(mvpView?.getActivity())
        val name = mvpView?.getPlanName()
        if (name?.isEmpty() == true) {
            mvpView?.showToast(R.string.create_plan_error_no_title)
        } else {
            savePlan()
        }
    }

    private fun savePlan() {
        if (mChangesMade) {
            // Save plan.
            val toSave = buildChangedItem()
            ELDatastore.planStore().create(toSave, SetOptions.merge())
            mvpView?.showToast(R.string.create_plan_saved)
            // Update local plan
            if (PlanManager.manager.isOngoing(toEdit)) {
                PlanManager.manager.setOngoingPlan(toEdit)
                sendBroadcast(Intent(ELConstants.BROADCAST_CURRENT_PLAN_CHANGED))
            }
            if (mEditMode) {
                AnalyticsManager.manager.planModified()
            } else {
                AnalyticsManager.manager.planCreated()
                AppLaunchManager.manager.rateActionTrigger()
                navigator.openPlanDetails(toEdit)
            }
        }
        mSavePressed = true
        mvpView?.closeScreen()
    }

    private fun handleImagePicked(url: String?) {
        toEdit?.imageUrl = url
        refreshInfo()
    }

    private fun changesMade() {
        toEdit?.name = mvpView.getPlanName()
        if (mInitialDataSet) {
            mChangesMade = true
        }
    }

    private fun handleChangeImage() {
        ELPickerDialog.withActivity(mvpView?.getActivity())
                .title(R.string.cover_picker_edit_image_title)
                .menuLayout(R.menu.menu_sheet_edit_cover_image)
                .actionListener { which ->
                    if (which == R.id.action_choose) {
                        navigator.openCoverImagePicker()
                    } else if (which == R.id.action_remove) {
                        handleImagePicked(null)
                    }
                }
                .show()
    }

    private fun handleAddWeek() {
        ELPickerDialog.withActivity(mvpView.getActivity())
                .title(R.string.create_plan_add_week_prompt)
                .menuLayout(R.menu.menu_sheet_plan_week_new)
                .actionListener { which ->
                    var week: ELPlanWeek? = null
                    if (which == R.id.action_copy_previous) {
                        week = toEdit?.duplicatePreviousWeek()
                    } else if (which == R.id.action_new) {
                        week = toEdit?.addNewWeek()
                    }
                    addWeek(week)
                }
                .show()
    }

    private fun handleRoutinePicked(routine: ELRoutine?) {
        toEdit!!.weeks[mSelectedWeek!!].getDays()[mSelectedDay!!].setRoutine(routine)
        refreshInfo()
        refreshLists()
        AnalyticsManager.manager.planWeekDaySetRoutine()
    }

    private fun showPanel(flow: CreatePlanActivity.Flow, weekNumber: Int = -1) {
        mShowingPanel = flow
        mvpView?.togglePanel(flow, weekNumber)
    }

    private fun addWeek(week: ELPlanWeek?) {
        if (week != null) {
            mWeeksDataListManager.add(week)
            AnalyticsManager.manager.planWeekAdded()
        } else {
            mvpView?.showToast(R.string.create_plan_error_max_weeks)
        }
        refreshInfo()
    }

    private fun deleteWeek(index: Int) {
        toEdit?.removeWeek(index)
        mWeeksDataListManager.remove(index)
        AnalyticsManager.manager.planWeekDeleted()
        mvpView?.showToast(R.string.create_plan_week_deleted)
        refreshInfo()
        Utils.runWithDelay({
            mWeeksAdapter.notifyDataSetChanged()
        }, 500)
    }

    private fun checkShouldDiscard() {
        if (!mSavePressed && !mEditMode) {
            ELDatastore.planStore().delete(toEdit!!)
            Utils.runWithDelay({
                closeScreen()
            }, 300)
        } else {
            closeScreen()
        }
    }

    private fun closeScreen() {
        mvpView?.setViewResult(Activity.RESULT_CANCELED)
        mvpView?.closeScreen()
    }

    // Panels

    private fun showInfo() {
        mSelectedDay = null
        mSelectedWeek = null
        showPanel(CreatePlanActivity.Flow.INFO)
    }

    private fun showWeeks() {
        mSelectedDay = null
        mSelectedWeek = null
        mWeeksDataListManager.clear()
        mWeeksDataListManager.addAll(toEdit?.weeks ?: ArrayList())
        mWeeksAdapter.notifyDataSetChanged()
        showPanel(CreatePlanActivity.Flow.WEEKS)
    }

    private fun showDays(week: ELPlanWeek?, weekIndex: Int) {
        mSelectedWeek = weekIndex
        mDaysDataListManager.clear()
        mDaysDataListManager.addAll(week?.getDays() ?: ArrayList())
        mDaysAdapter.notifyDataSetChanged()
        showPanel(CreatePlanActivity.Flow.DAYS, weekIndex)
    }

    private fun attemptBack(): Boolean {
        return when (mShowingPanel) {
            CreatePlanActivity.Flow.DAYS -> {
                if (canShowWeeksList()) {
                    showWeeks()
                } else {
                    showInfo()
                }
                true
            }
            CreatePlanActivity.Flow.WEEKS -> {
                showInfo()
                true
            }
            else -> false
        }
    }

    // Item changes

    private fun buildChangedItem(): ELPlan {
        toEdit?.name = mvpView.getPlanName()
        return toEdit!!
    }

    // Setup

    private fun setupState() {
        if (mSelectedWeek != null) {
            showDays(toEdit!!.weeks[mSelectedWeek!!], mSelectedWeek!!)
        } else if (mShowingPanel == CreatePlanActivity.Flow.WEEKS) {
            showWeeks()
        }
    }

    private fun setupEditedItem() {
        val uuid = mvpView.getItemToEditUuid()
        mEditMode = uuid != null
        if (uuid == null) {
            /*
             We're not in edit mode so create a new plan. This approach is used because Intents can
             only have a limited amount of information passed and plans are estimated to have lots
             of days and workouts attached, which will definitely go over at some point.
            */
            if (toEdit == null) {
                toEdit = ELPlan.newPlan(userAccount!!.id)
                ELDatastore.planStore().create(toEdit!!, SetOptions.merge())
            }
        }
    }

    private fun setupListViews() {
        // Weeks
        mWeeksAdapter.addDataManager(mWeeksDataListManager)
        mWeeksAdapter.registerBinder(PlanWeekAdapter.Binder(object : PlanWeekAdapter.OnPlanWeekListener {
            override fun onItemClicked(item: ELPlanWeek, position: Int) {
                showDays(item, position)
            }

            override fun onClickDuplicate(week: ELPlanWeek, position: Int) {
                addWeek(toEdit?.duplicateWeek(week))
            }

            override fun onClickDelete(week: ELPlanWeek, position: Int) {
                if (toEdit?.weeks?.size!! <= 1) {
                    mvpView?.showToast(R.string.create_plan_cannot_be_empty_prompt)
                } else {
                    deleteWeek(position)
                }
            }
        }))
        // Days
        mDaysAdapter.addDataManager(mDaysDataListManager)
        mDaysAdapter.registerBinder(PlanDayAdapter.Binder(object : PlanDayAdapter.OnPlanDayListener {
            override fun onClickChooseRoutine(day: ELPlanDay, position: Int) {
                mSelectedDay = position
                navigator.openRoutinePicker()
            }

            override fun onDayEdited() {
                changesMade()
            }

            override fun onItemClicked(item: ELPlanDay, position: Int) {}

            override fun onClickSkip(day: ELPlanDay) {}

            override fun onClickStart(day: ELPlanDay) {}
        }).setEdit(true))
    }
}