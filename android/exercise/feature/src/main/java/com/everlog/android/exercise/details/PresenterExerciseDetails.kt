package com.everlog.android.exercise.details

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.exercise.ExerciseAnalytics
import com.everlog.android.exercise.data.ExerciseImageStorage
import com.everlog.android.auth.data.UserSession
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.auth.data.requireUserId
import com.everlog.android.common.design.R as DesignR
import android.content.DialogInterface
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.exercise.data.store.ELUserExerciseStore
import com.everlog.android.exercise.data.store.ExerciseStores
import com.everlog.android.exercise.R
import com.everlog.android.exercise.create.CreateExerciseActivity
import com.everlog.android.exercise.details.statistics.ExerciseStatisticsFragment
import com.everlog.android.exercise.details.statistics.ExerciseStatsController
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.kotlin.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterExerciseDetails(
    private val settings: UserSettings,
    private val session: UserSession,
    private val imageStorage: ExerciseImageStorage,
    private val analytics: ExerciseAnalytics,
    private val workoutsStore: CollectionStore<ELWorkout>,
) : BaseActivityPresenter<MvpViewExerciseDetails>() {

    private var toEdit: ELExercise? = null
    private var mLastStats: ExerciseStatsController.StatsResult? = null
    private var mLoadedItemOnce = false
    private var mRange = RangeType.MONTH

    override fun onReady() {
        setupEditedItem()
        observeEditClick()
        observeDeleteClick()
        observeRangeChanged()
        loadExercise()
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
                .subscribe({ mvpView.getActivity()?.let { it.startActivityForResult(CreateExerciseActivity.editIntent(it, toEdit), CreateExerciseActivity.REQUEST_CODE) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeDeleteClick() {
        subscriptions.add(mvpView.onClickDelete()
                .compose(applyUISchedulers())
                .subscribe({ observeDeleteExerciseConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeDeleteExerciseConfirm() {
        subscriptions.add(mvpView.showDestructivePrompt(DesignR.string.delete_title, R.string.create_exercise_delete_prompt, DesignR.string.delete, DesignR.string.cancel)
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
                        analytics.statisticsRangeModified(range)
                    }
                })
    }

    // Loading

    private fun loadExercise() {
        // Show exercise immediately
        loadViewData()
        // Only the user's own exercises live in their exercises collection
        if (toEdit!!.isEditable(session.user()?.id)) {
            ExerciseStores.exerciseStore().getItem(toEdit!!.uuid)
        }
        // Load stats
        Utils.runWithDelay({
            if (isAttachedToView) {
                ExerciseStatsController(settings.weightUnit(), settings.firstDayOfWeek(), workoutsStore).loadStats(mRange,
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
        analytics.exerciseDeleted()
        ExerciseStores.exerciseStore().delete(toEdit!!)
        imageStorage.deleteExerciseImage(toEdit!!, session.requireUserId())
        mvpView.closeScreen()
    }

    // Setup

    private fun setupEditedItem() {
        toEdit = mvpView.getExercise()
    }
}