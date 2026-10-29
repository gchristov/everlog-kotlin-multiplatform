package com.everlog.ui.activities.home.exercise

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.exercises.ELExercisesStore.*
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.exercise.ExerciseSuggestion
import com.everlog.data.model.set.ELSetType
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.adapters.exercise.ExerciseAdapter
import com.everlog.ui.adapters.exercise.ExerciseAdapter.OnExerciseListener
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.views.revealcircle.FilterExercisesView
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.input.KeyboardUtils
import com.google.firebase.firestore.SetOptions
// import icepick.State
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import java.util.concurrent.TimeUnit

class PresenterExercises : BaseActivityPresenter<MvpViewExercises>() {

    // @JvmField
    // @State
    var mSelectedExercises = ArrayList<ELExercise>()

    private var mCacheLoaded = false // Exercise lists are merged so we can't know if it's from cache or not
    private val mExercises = ArrayList<ELExercise>()
    private var mLastFilter: FilterExercisesView.ExerciseFilters = FilterExercisesView.ExerciseFilters()

    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<Any>(mAdapter)

    override fun init() {
        super.init()
        setupListAdapters()
    }

    override fun onReady() {
        observeExercisesReady()
        observeSelectionActions()
        observeAddClick()
        observeSearchToggled()
        observeFiltersToggled()
        observeFiltersChanged()
        refreshSelectionState()
        loadExercises()
        checkOnboarding()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_SET_TYPE) {
            if (resultCode == Activity.RESULT_OK) {
                finishSelection(data?.getSerializableExtra(ELConstants.EXTRA_SET_TYPE) as ELSetType, true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseAdded(event: ELColStoreExerciseAddedEvent) {
        if (isAttachedToView && event.hasPendingWrites()) {
            ELDatastore.exercisesStore().getItems()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseModified(event: ELColStoreExerciseModifiedEvent) {
        if (isAttachedToView && event.hasPendingWrites()) {
            ELDatastore.exercisesStore().getItems()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseRemoved(event: ELColStoreExerciseRemovedEvent?) {
        if (isAttachedToView) {
            ELDatastore.exercisesStore().getItems()
        }
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Observers

    private fun observeExercisesReady() {
        subscriptions.add(ELDatastore.exercisesStore().observeAllItemsReady()
                .onBackpressureBuffer()
                .throttleLast(600, TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ items: List<ELExercise> ->
                    if (isAttachedToView) {
                        mExercises.clear()
                        mExercises.addAll(items)
                        if (mExercises.isNotEmpty() || mCacheLoaded) {
                            handleExercisesReady()
                        }
                        mCacheLoaded = true
                    }
                }) { throwable: Throwable? ->
                    if (isAttachedToView) {
                        mvpView.toggleLoadingOverlay(false)
                        checkEmptyState()
                        handleError(throwable)
                    }
                })
    }

    private fun observeSelectionActions() {
        subscriptions.add(mvpView.onClickSelectionLink()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openSetTypePicker(mSelectedExercises.size)
                }) { throwable: Throwable? -> handleError(throwable) })
        subscriptions.add(mvpView.onClickSelectionAdd()
                .compose(applyUISchedulers())
                .subscribe({
                    finishSelection(ELSetType.SINGLE, false)
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeAddClick() {
        subscriptions.add(Observable.merge(mvpView.onClickAdd(), mvpView.onClickEmptyAction())
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openEditExercise(null)
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeSearchToggled() {
        subscriptions.add(mvpView.onSearchHidden()
                .compose(applyUISchedulers())
                .subscribe({
                    handleSearchHidden()
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeFiltersToggled() {
        subscriptions.add(mvpView.onFiltersClick()
                .compose(applyUISchedulers())
                .subscribe({
                    mvpView?.toggleFilters(mLastFilter)
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeFiltersChanged() {
        subscriptions.add(mvpView.onSearchChanged()
                .compose(applyUISchedulers())
                .subscribe({ text ->
                    mLastFilter.title = if (TextUtils.isEmpty(text)) null else text
                    handleSearchChanged()
                }) { throwable: Throwable? -> handleError(throwable) })
        subscriptions.add(mvpView.onFiltersChanged()
                .compose(applyUISchedulers())
                .subscribe({ filters ->
                    mLastFilter.categories = filters?.categories ?: ArrayList()
                    mvpView?.toggleFilters(mLastFilter)
                    handleSearchChanged()
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeExerciseSuggestionCategory(suggestion: ExerciseSuggestion) {
        KeyboardUtils.hideKeyboard(mvpView.getActivity())
        val utils = ArrayResourceTypeUtils.withExerciseCategories()
        subscriptions.add(mvpView.showPickerMultipleChoice(utils.titles, -1, DialogBuilder.MultipleChoiceDialogType.EXERCISE_CATEGORY)
                .compose(applyUISchedulers())
                .subscribe({ value -> handleSaveExerciseSuggestion(suggestion, utils.types[value]) }, { throwable -> handleError(throwable) }))
    }

    // Loading

    private fun loadExercises() {
        if (mDataListManager.isEmpty) {
            mvpView.toggleLoadingOverlay(true)
        }
        // Load initial items.
        ELDatastore.exercisesStore().getItems()
    }

    // Handlers

    private fun handleOpenExercise(exercise: ELExercise) {
        if (mvpView.isSelectionMode()) {
            if (mSelectedExercises.contains(exercise)) {
                mSelectedExercises.remove(exercise)
            } else {
                // Make sure we can only add up to 20 exercises at a time
                val max = AppConfig.configuration.maxExerciseSelection
                if (mSelectedExercises.size >= max) {
                    mvpView.showToast(mvpView.context.getString(R.string.exercises_max_selection, max))
                    return
                }
                mSelectedExercises.add(exercise)
            }
            refreshSelectionState()
        } else {
            navigator.openExerciseDetails(ExerciseDetailsActivity.Companion.Properties()
                    .exercise(exercise))
        }
    }

    private fun handleExercisesReady() {
        val scrollPos = mvpView.getScrollPosition()
        handleSearchChanged()
        mvpView?.toggleLoadingOverlay(false)
        mvpView?.setScrollPosition(scrollPos)
        checkEmptyState()
    }

    private fun handleSearchHidden() {
        // Reset state
        mLastFilter.title = null
        handleExercisesReady()
    }

    private fun handleSearchChanged() {
        val filtered: List<Any> = ArrayList(mExercises)
                .filter { if (!TextUtils.isEmpty(mLastFilter.title)) it.name?.lowercase()?.contains(mLastFilter.title?.lowercase() ?: "") == true else true }
                .filter { exercise -> if (mLastFilter.categories.isNotEmpty()) mLastFilter.categories.map { it.type }.contains(exercise.category) else true }
        if (filtered.isEmpty() && !TextUtils.isEmpty(mLastFilter.title)) {
            (filtered as ArrayList).add(ExerciseSuggestion.newExerciseSuggestion(mLastFilter.title!!))
        }
        mDataListManager.set(filtered)
        mAdapter.notifyDataSetChanged()
        checkEmptyState()
    }

    private fun handleSaveExerciseSuggestion(suggestion: ExerciseSuggestion, category: String) {
        // Add new exercise directly to list for speed
        val toSave = ELExercise.newExercise(getUserAccount()!!.id!!, suggestion, category)
        mExercises.add(toSave)
        mLastFilter.categories.clear() // In case another category has been selected
        // Refresh list
        handleSearchChanged()
        // Save exercise
        ELDatastore.exerciseStore().create(toSave, SetOptions.merge())
        AnalyticsManager.manager.exerciseCreated()
        AnalyticsManager.manager.exerciseCreatedSuggestion()
        mvpView.showToast(R.string.create_exercise_saved)
        // Perform exercise tap-action
        handleOpenExercise(toSave)
    }

    private fun checkEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager.isEmpty, mLastFilter.title)
    }

    private fun refreshSelectionState() {
        mvpView?.toggleAddBtn(mSelectedExercises.isNotEmpty(), mSelectedExercises.size)
        mAdapter.notifyDataSetChanged()
    }

    private fun finishSelection(setType: ELSetType, link: Boolean) {
        val groups = ArrayList<ELExerciseGroup>()
        var group = ELExerciseGroup.buildDefault(setType)
        mSelectedExercises.forEach {
            val routineExercise = ELRoutineExercise.buildRoutineExercise(it)
            group.exercises.add(routineExercise)
            if (!link) {
                // If not linking, create a new group for each exercise
                groups.add(group)
                group = ELExerciseGroup.buildDefault(setType)
            }
        }
        if (link) {
            // If linking, return a single group
            groups.add(group)
        }
        val i = Intent()
        i.putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, groups)
        mvpView?.setViewResult(Activity.RESULT_OK, i)
        mvpView?.closeScreen()
    }

    // Onboarding

    private fun checkOnboarding() {
        mvpView?.getOnboardingController()?.checkOnboarding()
    }

    // Setup

    private fun setupListAdapters() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(ExerciseAdapter.Binder(mvpView.isSelectionMode(), object : OnExerciseListener {

            override fun getPreviousItem(position: Int): ELExercise? {
                return if (position > 0) (mDataListManager.get(position - 1) as? ELExercise) else null
            }

            override fun isSelected(exercise: ELExercise): Boolean {
                return mSelectedExercises.contains(exercise)
            }

            override fun onItemClicked(item: ELExercise, position: Int) {
                handleOpenExercise(item)
            }
        }))
        mAdapter.registerBinder(ExerciseAdapter.SuggestionBinder { item, _ -> observeExerciseSuggestionCategory(item) })
    }
}