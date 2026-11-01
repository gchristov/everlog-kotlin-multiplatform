package com.everlog.managers.analytics

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.managers.appupdate.AppUpdateController

interface Analytic {

    fun toggleAnalytics(enabled: Boolean)

    fun remoteConfigFetched()

    fun notificationHomeShown(title: String?)
    fun notificationHomeTapped(title: String?)
    fun notificationHomeDismissed(title: String?)
    fun appUsageReminderShown(attempt: Int, title: String?)
    fun appUsageReminderOpened(attempt: Int, title: String?)

    fun ratePromptTriggered(source: String, promptNumber: Int)
    fun feedbackOpened(source: String)

    fun screenName(screenName: String?)

    fun userRegister(userId: String?, method: String)

    fun userLogin(userId: String?, method: String)

    fun userIdentify(userId: String?)

    fun userLogout()

    fun routineCreated()
    fun routineModified()
    fun routineDeleted()
    fun routineExerciseGroupAdded()
    fun routineExerciseModified()
    fun routineLeavePromptShown()
    fun routineLeavePromptSaved()
    fun routineLeavePromptDiscarded()
    fun routineLeavePromptCancelled()

    fun setTypeSelected(type: ELSetType?)
    fun setWeightModified(value: Float?)
    fun setRequiredRepsModified(value: Int?)
    fun setRepsModified(value: Int?)
    fun setRequiredTimeModified(value: Int?)
    fun setTimeModified(value: Int?)
    fun setRestTimeModified(value: Int?)
    fun setAdded()
    fun setDeleted()
    fun setCompleted(source: String, type: ELSetType?, timed: Boolean)
    fun setGroupCompleted(source: String, type: ELSetType?, exercises: Int)
    fun setEdited(type: ELSetType?, timed: Boolean)

    fun statisticsRangeModified(rangeType: RangeType)

    fun settingsMuscleGoalModified(value: MuscleGoal?)
    fun settingsWeightModified(value: Float?)
    fun settingsWeeklyGoalModified(value: Int?)
    fun settingsRestTimeModified(value: Int?)
    fun settingsWeightUnitModified(value: String?)
    fun settingsFirstWeekDayModified(value: String?)
    fun settingsKeepScreenOnModified(value: Boolean)

    fun homeAddFabTapped()
    fun homeAddWeekEmptyStateTapped()

    fun appUpdatePromptShown(versionCode: Int)
    fun appUpdateAccepted(versionCode: Int)
    fun appUpdateDeclined(versionCode: Int)
    fun appUpdateDownloaded()
    fun appUpdateRestartTapped()
    fun appUpdateFailed(failure: AppUpdateController.Failure, errorCode: Int)

    fun workoutStarted(source: String)
    fun workoutStopped()
    fun workoutCompleted()
    fun workoutDiscardPromptShown(source: String, setsCompleted: Int)
    fun workoutDiscardPromptFinished()
    fun workoutDiscardPromptCancelled(source: String)
    fun workoutResumed(source: String, setsCompleted: Int)
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
    fun workoutServiceTimerStartedExercise()
    fun workoutServiceTimerStoppedExercise()
    fun workoutServiceTimerStoppedRest()
    fun workoutChangeMuscleGoal()
    fun workoutKeypadShown(source: String)
    fun workoutKeypadHidden(source: String)
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

    fun consentNewsletterGranted()
    fun consentNewsletterDenied()

    fun onboardingStepViewed(step: String)
    fun onboardingStepCompleted(step: String, value: String?)
    fun onboardingStepSkipped(step: String)
    fun onboardingStepFailed(step: String)
    fun onboardingStepRetried(step: String)
    fun onboardingQuestionReopened(step: String)
    fun onboardingQuestionEdited(step: String, value: String?)
    fun onboardingSkipPromptShown(step: String)
    fun onboardingSkipPromptCancelled(step: String)
    fun onboardingTemplateToggled(open: Boolean)
    fun onboardingFinished(outcome: String, step: String, routines: Int)

    // The new exercise picker (TAS-451); the screen view says which mode
    fun exercisePickerFilterAdded(filter: String, value: String, source: String)
    fun exercisePickerFilterRemoved(filter: String)
    fun exercisePickerFilterOperatorChanged(filter: String, not: Boolean)
    fun exercisePickerSearched(words: Int, results: Int)
    fun exercisePickerNoResults(text: String?, filters: Int)
    fun exercisePickerExercisePicked(section: String, position: Int)
    fun exercisePickerCreateTapped(source: String)
    fun exercisePickerAdded(count: Int, setType: String, filters: Int, hadText: Boolean)

    fun setSuggestionsHowOpened()
}
