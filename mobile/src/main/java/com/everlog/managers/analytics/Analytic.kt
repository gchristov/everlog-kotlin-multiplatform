package com.everlog.managers.analytics

import android.app.Activity
import com.everlog.data.model.set.ELSetType
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment.RangeType

interface Analytic {

    fun toggleAnalytics(enabled: Boolean)

    fun remoteConfigFetched()

    fun notificationHomeDismissed()

    fun appStarRating(value: Float?)
    fun appRate()
    fun appFeedback()
    fun appNotNow()

    fun screenName(activity: Activity?, screenName: String?)

    fun userRegister(userId: String?,
                     email: String?,
                     displayName: String?)

    fun userIdentify(userId: String?,
                     email: String?,
                     displayName: String?)

    fun userLogout()

    fun routineCreated()
    fun routineModified()
    fun routineDeleted()
    fun routineExerciseGroupAdded()
    fun routineExerciseModified()

    fun setTypeSelected(type: ELSetType?)
    fun setWeightModified(value: Float?)
    fun setRequiredRepsModified(value: Int?)
    fun setRepsModified(value: Int?)
    fun setRequiredTimeModified(value: Int?)
    fun setTimeModified(value: Int?)
    fun setRestTimeModified(value: Int?)
    fun setModified()
    fun setAdded()
    fun setDeleted()

    fun statisticsRangeModified(rangeType: RangeType)

    fun settingsMuscleGoalModified(value: MuscleGoal?)
    fun settingsWeightModified(value: Float?)
    fun settingsWeeklyGoalModified(value: Int?)
    fun settingsRestTimeModified(value: Int?)
    fun settingsWeightUnitModified(value: String?)
    fun settingsFirstWeekDayModified(value: String?)

    fun workoutStarted()
    fun workoutQuickStarted()
    fun workoutFromRoutineStarted()
    fun workoutStopped()
    fun workoutCompleted()
    fun workoutNextExercise()
    fun workoutPrevExercise()
    fun workoutRepsModified()
    fun workoutTimeModified()
    fun workoutWeightModified()
    fun workoutServiceNextExercise()
    fun workoutServicePrevExercise()
    fun workoutServiceRepsModified()
    fun workoutServiceWeightModified()
    fun workoutServiceWorkoutCompleted()
    fun workoutChangeMuscleGoal()
    fun workoutTimerStartedExercise()
    fun workoutTimerStoppedExercise()
    fun workoutTimerStartedRest()
    fun workoutTimerStoppedRest()

    fun workoutDetailsNameModified()
    fun workoutDetailsDateModified()
    fun workoutDetailsNotesModified()
    fun workoutDetailsExercisesModified()
    fun workoutDetailsShared()
    fun workoutDetailsSavedAsRoutine()
    fun workoutDetailsDeleted()

    fun exerciseCreated()
    fun exerciseCreatedSuggestion()
    fun exerciseModified()
    fun exerciseDeleted()

    fun proBuyMonth()
    fun proBuyYear()
    fun proBuyPurchased()
    fun proBuyCancelled()
    fun proBuyAlreadyOwned()
    fun proBuyRestore()
    fun proManageSubscription()

    fun planCreated()
    fun planModified()
    fun planDeleted()
    fun planWeekAdded()
    fun planWeekDeleted()
    fun planWeekDaySetRoutine()
    fun planWeekDaySetRest()
    fun planStarted()
    fun planStopped()
    fun planCompleted()
    fun planDayComplete()
    fun planCoverModified()

    fun integrationAccessRequested()
    fun integrationConnected()
    fun integrationDisconnected()
    fun integrationSyncRequested()
    fun integrationUnsyncRequested()

    fun consentNewsletterGranted()
    fun consentNewsletterDenied()
}