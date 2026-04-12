package com.everlog.ui.activities.home.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.services.workout.WorkoutService
import com.everlog.ui.activities.home.exercisegroup.CreateExerciseGroupsActivity
import com.everlog.ui.activities.home.exercisegroup.PresenterCreateExerciseGroups
import com.everlog.ui.onboarding.WorkoutOnboardingController
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject

class WorkoutActivity : CreateExerciseGroupsActivity(), MvpViewWorkout, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mPresenter: PresenterWorkout? = null
    lateinit var workoutBinding: ActivityWorkoutBinding

    private var mMuscleGoalsVisible = true

    private val mOnRequestWeightIncrease = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestWeightDecrease = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestRepsIncrease = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestRepsDecrease = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestTimerExerciseStart = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestTimerExerciseStop = PublishSubject.create<ELWorkoutState>()
    private val mOnRequestTimerRestStop = PublishSubject.create<Void>()
    private val mOnRequestCompleteSet = PublishSubject.create<ELWorkoutState>()

    // Workout Service

    private var mReceiver: WorkoutServiceBroadcastReceiver? = null

    override fun onActivityCreated() {
        super.onActivityCreated()
        setupBroadcastReceivers()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        mPresenter?.onRequestPermissionsResult(requestCode)
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_WORKOUT
    }

    override fun onDestroy() {
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver!!)
        }
        super.onDestroy()
    }

    override fun getLayoutResId(): Int {
        return 0
    }

    override fun getBindingView(): View? {
        workoutBinding = ActivityWorkoutBinding.inflate(layoutInflater)
        return workoutBinding.root
    }

    override fun getBinding(): ActivityWorkoutBinding {
        return workoutBinding
    }

    override fun getExerciseGroupsPresenter(): PresenterCreateExerciseGroups<*>? {
        return mPresenter
    }

    override fun onClickMuscleGoalStatus(): Observable<Void> {
        return RxView.clicks(workoutBinding.muscleGoalBtn)
    }

    override fun onClickEmptyAction(): Observable<Void> {
        return workoutBinding.emptyView.onActionClick()
    }

    override fun getWorkout(): ELWorkout? {
        return intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as? ELWorkout
    }

    override fun performingFromPlan(): Boolean {
        return intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false)
    }

    override fun onClickServiceIncreaseWeight(): Observable<ELWorkoutState> {
        return mOnRequestWeightIncrease
    }

    override fun onClickServiceDecreaseWeight(): Observable<ELWorkoutState> {
        return mOnRequestWeightDecrease
    }

    override fun onClickServiceIncreaseReps(): Observable<ELWorkoutState> {
        return mOnRequestRepsIncrease
    }

    override fun onClickServiceDecreaseReps(): Observable<ELWorkoutState> {
        return mOnRequestRepsDecrease
    }

    override fun onClickServiceTimerExerciseStart(): Observable<ELWorkoutState> {
        return mOnRequestTimerExerciseStart
    }

    override fun onClickServiceTimerExerciseStop(): Observable<ELWorkoutState> {
        return mOnRequestTimerExerciseStop
    }

    override fun onClickServiceTimerRestStop(): Observable<Void> {
        return mOnRequestTimerRestStop
    }

    override fun onClickServiceCompleteSet(): Observable<ELWorkoutState> {
        return mOnRequestCompleteSet
    }

    override fun loadWorkoutDetails(workout: ELWorkout, hasExercises: Boolean) {
        // Empty state
        workoutBinding.emptyView.visibility = if (!hasExercises) View.VISIBLE else View.GONE
        // Exercises
        val contentVisibility = if (workoutBinding.emptyView.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        arrayOf(workoutBinding.recyclerView, workoutBinding.muscleGoalBtn, workoutBinding.addBtnContainer).forEach { it.visibility = contentVisibility }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterWorkout()
    }

    override fun setupTopBar() {
        super.setupTopBar()
        supportActionBar?.title = "00:00"
        workoutBinding.topBar.actionBar.saveBtn.setText(R.string.workout_finish)
    }

    override fun setupListView() {
        super.setupListView()
        workoutBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
                super.onScrolled(recyclerView, dX, dY)
                // Hide muscle goal button if recycler view is on top of it
                mMuscleGoalsVisible = if (recyclerView.computeVerticalScrollOffset() > (workoutBinding.muscleGoalBtn.height ?: 0)/2) {
                    if (mMuscleGoalsVisible) {
                        workoutBinding.muscleGoalBtn.animate()?.alpha(0f)
                    }
                    false
                } else {
                    if (!mMuscleGoalsVisible) {
                        workoutBinding.muscleGoalBtn.animate()?.alpha(1f)
                    }
                    true
                }
            }
        })
    }

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(WorkoutService.BROADCAST_REQUEST_COMPLETE_SET)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_INCREASE_WEIGHT)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_DECREASE_WEIGHT)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_INCREASE_REPS)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_DECREASE_REPS)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_TIMER_REST_STOP)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_START)
        filter.addAction(WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_STOP)
        mReceiver = WorkoutServiceBroadcastReceiver()
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver!!, filter)
    }

    override fun setupOnboarding() {
        mOnboardingController = WorkoutOnboardingController(this)
    }

    private inner class WorkoutServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (WorkoutService.BROADCAST_REQUEST_INCREASE_WEIGHT == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestWeightIncrease.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_DECREASE_WEIGHT == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestWeightDecrease.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_INCREASE_REPS == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestRepsIncrease.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_DECREASE_REPS == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestRepsDecrease.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_START == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestTimerExerciseStart.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_STOP == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestTimerExerciseStop.onNext(state)
                }
            } else if (WorkoutService.BROADCAST_REQUEST_TIMER_REST_STOP == intent?.action) {
                mOnRequestTimerRestStop.onNext(null)
            } else if (WorkoutService.BROADCAST_REQUEST_COMPLETE_SET == intent?.action) {
                val state = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState
                if (state != null) {
                    mOnRequestCompleteSet.onNext(state)
                }
            }
        }
    }
}