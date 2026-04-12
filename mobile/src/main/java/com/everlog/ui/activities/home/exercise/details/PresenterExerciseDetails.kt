package com.everlog.ui.activities.home.exercise.details

import android.content.DialogInterface
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.ELUserStore
import com.everlog.data.datastores.exercises.ELUserExerciseStore
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.firebase.FirebaseStorageManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.fragments.home.exercise.statistics.ExerciseStatisticsFragment
import com.everlog.utils.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterExerciseDetails : BaseActivityPresenter<MvpViewExerciseDetails>() {

    private var toEdit: ELExercise? = null
    private var mLastStats: ExerciseStatsController.StatsResult? = null
    private var mLoadedItemOnce = false
    private var mRange = AppConfig.configuration.defaultStatsExerciseRange

    override fun onReady() {
        setupEditedItem()
        observeEditClick()
        observeDeleteClick()
        observeRangeChanged()
        loadExercise()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserLoaded(event: ELUserStore.ELDocStoreUserLoadedEvent) {
        if (isAttachedToView) {
            loadViewData()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseLoaded(event: ELUserExerciseStore.ELDocStoreExerciseLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                handleError(event.error)
            } else {
                if (!mLoadedItemOnce || event.isHasPendingWrites) {
                    handleAddOrEditExerciseEvent(event.item)
                }
                mLoadedItemOnce = true
            }
        }
    }

    // Observers

    private fun observeEditClick() {
        subscriptions.add(mvpView.onClickEdit()
                .compose(applyUISchedulers())
                .subscribe({ navigator.openEditExercise(toEdit) }, { throwable -> handleError(throwable) }))
    }

    private fun observeDeleteClick() {
        subscriptions.add(mvpView.onClickDelete()
                .compose(applyUISchedulers())
                .subscribe({ observeDeleteExerciseConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeDeleteExerciseConfirm() {
        subscriptions.add(mvpView.showPrompt(R.string.delete_title, R.string.create_exercise_delete_prompt, R.string.delete, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleDeleteExercise()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeRangeChanged() {
        subscriptions.add(ExerciseStatisticsFragment.onRangeChanged
                .compose(applyUISchedulers())
                .subscribe { range ->
                    if (mRange != range) {
                        mRange = range
                        mLastStats = null
                        loadExercise()
                        AnalyticsManager.manager.statisticsRangeModified(range)
                    }
                })
    }

    // Loading

    private fun loadExercise() {
        // Show exercise immediately
        loadViewData()
        // Listen for item changes
        ELDatastore.exerciseStore().getItem(toEdit!!.uuid)
        // Load stats
        Utils.runWithDelay({
            if (isAttachedToView) {
                ExerciseStatsController().loadStats(mRange,
                        toEdit!!,
                        object : ExerciseStatsController.OnCompleteListener {
                            override fun onComplete(result: ExerciseStatsController.StatsResult) {
                                mLastStats = result
                                loadViewData()
                            }

                            override fun onError(throwable: Throwable) {
                                // Show empty stats, shouldn't happen
                                mLastStats = ExerciseStatsController.StatsResult()
                                loadViewData()
                                handleError(throwable)
                            }
                        })
            }
        }, 900)
    }

    private fun loadViewData() {
        mvpView?.loadExerciseDetails(toEdit!!, mLastStats, mRange)
    }

    // Handlers

    private fun handleAddOrEditExerciseEvent(exercise: ELExercise?) {
        // Make sure we are looking at the correct item.
        if (isAttachedToView && exercise?.uuid == toEdit?.uuid) {
            toEdit = exercise
            loadViewData()
        }
    }

    private fun handleDeleteExercise() {
        AnalyticsManager.manager.exerciseDeleted()
        ELDatastore.exerciseStore().delete(toEdit!!)
        FirebaseStorageManager.deleteExerciseImage(toEdit!!)
        mvpView.closeScreen()
    }

    // Setup

    private fun setupEditedItem() {
        toEdit = mvpView.getExercise()
    }
}