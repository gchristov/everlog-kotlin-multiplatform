package com.everlog.ui.activities.home.workout.utils

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import com.everlog.config.AppConfig
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.WorkoutManager
import com.everlog.ui.activities.home.workout.MvpViewWorkout
import com.everlog.ui.views.revealcircle.WorkoutTimerView
import rx.Observable
import rx.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

abstract class BaseWorkoutTimeController(
    mvpView: MvpViewWorkout?,
    workout: ELWorkout,
    private val contentPanel: ViewGroup
) {

    private var mTimerView: WorkoutTimerView? = null

    internal val mMvpView = mvpView
    internal val mWorkout = workout

    internal var mTotalTimeSeconds = 0
    private var mStartedDate = 0L

    private val mTimeStopped = PublishSubject.create<Void>()

    abstract fun timerStarted()

    abstract fun timerStopped(userCancelled: Boolean)

    abstract fun timerOffsetUpdated()

    abstract fun timerTick(remainingSeconds: Int, remainingProgress: Int)

    abstract fun getTimerTitle(): String

    abstract fun buildTimer(context: Context): WorkoutTimerView

    fun onTimeStopped(): Observable<Void> {
        return mTimeStopped
    }

    fun startTimer(timeSeconds: Int) {
        mStartedDate = Date().time
        mTotalTimeSeconds = timeSeconds
        if (isActive()) {
            attachTimer()
            // Schedule notification for workout timer
            WorkoutManager.manager.setOngoingTimer(true, mTotalTimeSeconds)
            timerStarted()
        }
    }

    fun stopTimer(userCancelled: Boolean) {
        mTimerView?.hide(contentPanel)
        mTimerView = null
        mStartedDate = 0
        mTotalTimeSeconds = 0
        mTimeStopped.onNext(null)
        if (userCancelled) {
            // Cancel notification for workout timer
            WorkoutManager.manager.setOngoingTimer(false)
        }
        timerStopped(userCancelled)
    }

    fun isActive(): Boolean {
        return mTotalTimeSeconds > 0
    }

    fun workoutTimerTick() {
        attachTimer()
        applyListeners()
        val remaining = mTotalTimeSeconds - TimeUnit.MILLISECONDS.toSeconds(Date().time - mStartedDate).toInt()
        if (remaining <= 0) {
            stopTimer(false)
        } else {
            val percentRemaining = (remaining * 100) / mTotalTimeSeconds
            mTimerView?.updateTime(getTimerTitle(), remaining, percentRemaining)
            timerTick(remaining, percentRemaining)
        }
    }

    private fun timeOffsetModified(offset: Int) {
        mTotalTimeSeconds += offset
        if (isActive()) {
            timerOffsetUpdated()
            WorkoutManager.manager.setOngoingTimer(true, mTotalTimeSeconds)
        }
        workoutTimerTick()
    }

    private fun applyListeners() {
        mTimerView?.observeIncreaseClick(View.OnClickListener {
            timeOffsetModified(AppConfig.configuration.defaultRestTimeOffsetSeconds)
        })
        mTimerView?.observeDecreaseClick(View.OnClickListener {
            timeOffsetModified(-AppConfig.configuration.defaultRestTimeOffsetSeconds)
        })
        mTimerView?.observeCancelClick(View.OnClickListener {
            stopTimer(true)
        })
    }

    private fun attachTimer() {
        if (mTimerView == null) {
            mTimerView = buildTimer(mMvpView!!.context)
            mTimerView?.show(contentPanel, Point(contentPanel.width / 2, contentPanel.height / 2))
        }
        // Initial timer state
        mTimerView?.updateTime(getTimerTitle(), mTotalTimeSeconds, 100)
    }
}