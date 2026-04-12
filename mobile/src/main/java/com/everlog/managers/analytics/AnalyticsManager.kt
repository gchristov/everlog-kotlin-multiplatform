package com.everlog.managers.analytics

import android.app.Activity
import com.everlog.data.model.set.ELSetType
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import timber.log.Timber
import java.util.*

class AnalyticsManager : Analytic {

    private val TAG = "AnalyticsManager"

    private var mAnalytics: MutableList<Analytic>? = null

    companion object {

        @JvmField
        val manager = AnalyticsManager()
    }

    init {
        mAnalytics = ArrayList()
        mAnalytics?.add(FirebaseAnalytic())
    }

    fun initialize() {
        // No-op
    }

    override fun toggleAnalytics(enabled: Boolean) {
        Timber.tag(TAG).i("Analytics toggled: enabled=%s", enabled)
        mAnalytics?.forEach {
            it.toggleAnalytics(enabled)
        }
    }

    override fun remoteConfigFetched() {
        Timber.tag(TAG).i("Remote config fetched")
        mAnalytics?.forEach {
            it.remoteConfigFetched()
        }
    }

    override fun notificationHomeDismissed() {
        Timber.tag(TAG).i("Notification home dismissed")
        mAnalytics?.forEach {
            it.notificationHomeDismissed()
        }
    }

    override fun appRate() {
        Timber.tag(TAG).i("App rated")
        mAnalytics?.forEach {
            it.appRate()
        }
    }

    override fun appStarRating(value: Float?) {
        Timber.tag(TAG).i("App star rating: value=%f", value)
        mAnalytics?.forEach {
            it.appStarRating(value)
        }
    }

    override fun appFeedback() {
        Timber.tag(TAG).i("App feedback")
        mAnalytics?.forEach {
            it.appFeedback()
        }
    }

    override fun appNotNow() {
        Timber.tag(TAG).i("App not now")
        mAnalytics?.forEach {
            it.appNotNow()
        }
    }

    override fun screenName(activity: Activity?, screenName: String?) {
        Timber.tag(TAG).i("Screen name: activity=%s, screenName=%s", activity?.javaClass?.simpleName, screenName)
        mAnalytics?.forEach {
            it.screenName(activity, screenName)
        }
    }

    override fun userRegister(userId: String?,
                              email: String?,
                              displayName: String?) {
        Timber.tag(TAG).i("User register: userId=%s, email=%s, displayName=%s", userId, email, displayName)
        mAnalytics?.forEach {
            it.userRegister(userId, email, displayName)
        }
    }

    override fun userIdentify(userId: String?,
                              email: String?,
                              displayName: String?) {
        Timber.tag(TAG).i("User identify: userId=%s, email=%s, displayName=%s", userId, email, displayName)
        mAnalytics?.forEach {
            it.userIdentify(userId, email, displayName)
        }
    }

    override fun userLogout() {
        Timber.tag(TAG).i("User logout")
        mAnalytics?.forEach {
            it.userLogout()
        }
    }

    override fun routineCreated() {
        Timber.tag(TAG).i("Routine created")
        mAnalytics?.forEach {
            it.routineCreated()
        }
    }

    override fun routineModified() {
        Timber.tag(TAG).i("Routine modified")
        mAnalytics?.forEach {
            it.routineModified()
        }
    }

    override fun routineDeleted() {
        Timber.tag(TAG).i("Routine deleted")
        mAnalytics?.forEach {
            it.routineDeleted()
        }
    }

    override fun routineExerciseGroupAdded() {
        Timber.tag(TAG).i("Routine exercise group added")
        mAnalytics?.forEach {
            it.routineExerciseGroupAdded()
        }
    }

    override fun routineExerciseModified() {
        Timber.tag(TAG).i("Routine exercise modified")
        mAnalytics?.forEach {
            it.routineExerciseModified()
        }
    }

    override fun setTypeSelected(type: ELSetType?) {
        Timber.tag(TAG).i("Set type selected: type=%s", type?.name)
        mAnalytics?.forEach {
            it.setTypeSelected(type)
        }
    }

    override fun setWeightModified(value: Float?) {
        Timber.tag(TAG).i("Set weight modified: value=%s", value)
        mAnalytics?.forEach {
            it.setWeightModified(value)
        }
    }

    override fun setRequiredRepsModified(value: Int?) {
        Timber.tag(TAG).i("Set required reps modified: value=%s", value)
        mAnalytics?.forEach {
            it.setRequiredRepsModified(value)
        }
    }

    override fun setRepsModified(value: Int?) {
        Timber.tag(TAG).i("Set reps modified: value=%s", value)
        mAnalytics?.forEach {
            it.setRepsModified(value)
        }
    }

    override fun setRequiredTimeModified(value: Int?) {
        Timber.tag(TAG).i("Set required time modified: value=%s", value)
        mAnalytics?.forEach {
            it.setRequiredTimeModified(value)
        }
    }

    override fun setTimeModified(value: Int?) {
        Timber.tag(TAG).i("Set time modified: value=%s", value)
        mAnalytics?.forEach {
            it.setTimeModified(value)
        }
    }

    override fun setRestTimeModified(value: Int?) {
        Timber.tag(TAG).i("Set rest time modified: value=%s", value)
        mAnalytics?.forEach {
            it.setRestTimeModified(value)
        }
    }

    override fun setModified() {
        Timber.tag(TAG).i("Set modified")
        mAnalytics?.forEach {
            it.setModified()
        }
    }

    override fun setAdded() {
        Timber.tag(TAG).i("Set added")
        mAnalytics?.forEach {
            it.setAdded()
        }
    }

    override fun setDeleted() {
        Timber.tag(TAG).i("Set deleted")
        mAnalytics?.forEach {
            it.setDeleted()
        }
    }

    override fun statisticsRangeModified(rangeType: StatisticsHomeFragment.RangeType) {
        Timber.tag(TAG).i("Statistics range modified: type=%s", rangeType)
        mAnalytics?.forEach {
            it.statisticsRangeModified(rangeType)
        }
    }

    override fun settingsMuscleGoalModified(value: MuscleGoal?) {
        Timber.tag(TAG).i("Settings muscle goal modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsMuscleGoalModified(value)
        }
    }

    override fun settingsWeightModified(value: Float?) {
        Timber.tag(TAG).i("Settings weight modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsWeightModified(value)
        }
    }

    override fun settingsWeeklyGoalModified(value: Int?) {
        Timber.tag(TAG).i("Settings weekly goal modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsWeeklyGoalModified(value)
        }
    }

    override fun settingsRestTimeModified(value: Int?) {
        Timber.tag(TAG).i("Settings rest time modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsRestTimeModified(value)
        }
    }

    override fun settingsWeightUnitModified(value: String?) {
        Timber.tag(TAG).i("Settings weight unit modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsWeightUnitModified(value)
        }
    }

    override fun settingsFirstWeekDayModified(value: String?) {
        Timber.tag(TAG).i("Settings first week day modified: value=%s", value)
        mAnalytics?.forEach {
            it.settingsFirstWeekDayModified(value)
        }
    }

    override fun workoutStarted() {
        Timber.tag(TAG).i("Workout started")
        mAnalytics?.forEach {
            it.workoutStarted()
        }
    }

    override fun workoutQuickStarted() {
        Timber.tag(TAG).i("Workout quick started")
        mAnalytics?.forEach {
            it.workoutQuickStarted()
        }
    }

    override fun workoutFromRoutineStarted() {
        Timber.tag(TAG).i("Workout from routine started")
        mAnalytics?.forEach {
            it.workoutFromRoutineStarted()
        }
    }

    override fun workoutStopped() {
        Timber.tag(TAG).i("Workout stopped")
        mAnalytics?.forEach {
            it.workoutStopped()
        }
    }

    override fun workoutCompleted() {
        Timber.tag(TAG).i("Workout completed")
        mAnalytics?.forEach {
            it.workoutCompleted()
        }
    }

    override fun workoutNextExercise() {
        Timber.tag(TAG).i("Workout next exercise")
        mAnalytics?.forEach {
            it.workoutNextExercise()
        }
    }

    override fun workoutPrevExercise() {
        Timber.tag(TAG).i("Workout previous exercise")
        mAnalytics?.forEach {
            it.workoutPrevExercise()
        }
    }

    override fun workoutRepsModified() {
        Timber.tag(TAG).i("Workout reps modified")
        mAnalytics?.forEach {
            it.workoutRepsModified()
        }
    }

    override fun workoutTimeModified() {
        Timber.tag(TAG).i("Workout time modified")
        mAnalytics?.forEach {
            it.workoutTimeModified()
        }
    }

    override fun workoutWeightModified() {
        Timber.tag(TAG).i("Workout weight modified")
        mAnalytics?.forEach {
            it.workoutWeightModified()
        }
    }

    override fun workoutServiceRepsModified() {
        Timber.tag(TAG).i("Workout service reps modified")
        mAnalytics?.forEach {
            it.workoutServiceRepsModified()
        }
    }

    override fun workoutServiceWeightModified() {
        Timber.tag(TAG).i("Workout service weight modified")
        mAnalytics?.forEach {
            it.workoutServiceWeightModified()
        }
    }

    override fun workoutServiceNextExercise() {
        Timber.tag(TAG).i("Workout service next exercise")
        mAnalytics?.forEach {
            it.workoutServiceNextExercise()
        }
    }

    override fun workoutServicePrevExercise() {
        Timber.tag(TAG).i("Workout service prev exercise")
        mAnalytics?.forEach {
            it.workoutServicePrevExercise()
        }
    }

    override fun workoutServiceWorkoutCompleted() {
        Timber.tag(TAG).i("Workout service workout completed")
        mAnalytics?.forEach {
            it.workoutServiceWorkoutCompleted()
        }
    }

    override fun workoutChangeMuscleGoal() {
        Timber.tag(TAG).i("Workout change muscle goal")
        mAnalytics?.forEach {
            it.workoutChangeMuscleGoal()
        }
    }

    override fun workoutTimerStartedExercise() {
        Timber.tag(TAG).i("Workout timer started exercise")
        mAnalytics?.forEach {
            it.workoutTimerStartedExercise()
        }
    }

    override fun workoutTimerStoppedExercise() {
        Timber.tag(TAG).i("Workout timer stopped exercise")
        mAnalytics?.forEach {
            it.workoutTimerStoppedExercise()
        }
    }

    override fun workoutTimerStartedRest() {
        Timber.tag(TAG).i("Workout timer started rest")
        mAnalytics?.forEach {
            it.workoutTimerStartedRest()
        }
    }

    override fun workoutTimerStoppedRest() {
        Timber.tag(TAG).i("Workout timer stopped rest")
        mAnalytics?.forEach {
            it.workoutTimerStoppedRest()
        }
    }

    override fun workoutDetailsNameModified() {
        Timber.tag(TAG).i("Workout details name modified")
        mAnalytics?.forEach {
            it.workoutDetailsNameModified()
        }
    }

    override fun workoutDetailsDateModified() {
        Timber.tag(TAG).i("Workout details date modified")
        mAnalytics?.forEach {
            it.workoutDetailsDateModified()
        }
    }

    override fun workoutDetailsNotesModified() {
        Timber.tag(TAG).i("Workout details notes modified")
        mAnalytics?.forEach {
            it.workoutDetailsNotesModified()
        }
    }

    override fun workoutDetailsExercisesModified() {
        Timber.tag(TAG).i("Workout details exercises modified")
        mAnalytics?.forEach {
            it.workoutDetailsExercisesModified()
        }
    }

    override fun workoutDetailsShared() {
        Timber.tag(TAG).i("Workout details shared")
        mAnalytics?.forEach {
            it.workoutDetailsShared()
        }
    }

    override fun workoutDetailsSavedAsRoutine() {
        Timber.tag(TAG).i("Workout details saved as routine")
        mAnalytics?.forEach {
            it.workoutDetailsSavedAsRoutine()
        }
    }

    override fun workoutDetailsDeleted() {
        Timber.tag(TAG).i("Workout details deleted")
        mAnalytics?.forEach {
            it.workoutDetailsDeleted()
        }
    }

    override fun exerciseCreated() {
        Timber.tag(TAG).i("Exercise created")
        mAnalytics?.forEach {
            it.exerciseCreated()
        }
    }

    override fun exerciseCreatedSuggestion() {
        Timber.tag(TAG).i("Exercise created suggestion")
        mAnalytics?.forEach {
            it.exerciseCreatedSuggestion()
        }
    }

    override fun exerciseModified() {
        Timber.tag(TAG).i("Exercise modified")
        mAnalytics?.forEach {
            it.exerciseModified()
        }
    }

    override fun exerciseDeleted() {
        Timber.tag(TAG).i("Exercise deleted")
        mAnalytics?.forEach {
            it.exerciseDeleted()
        }
    }

    override fun proBuyMonth() {
        Timber.tag(TAG).i("Pro buy month")
        mAnalytics?.forEach {
            it.proBuyMonth()
        }
    }

    override fun proBuyYear() {
        Timber.tag(TAG).i("Pro buy year")
        mAnalytics?.forEach {
            it.proBuyYear()
        }
    }

    override fun proBuyPurchased() {
        Timber.tag(TAG).i("Pro buy purchased")
        mAnalytics?.forEach {
            it.proBuyPurchased()
        }
    }

    override fun proBuyCancelled() {
        Timber.tag(TAG).i("Pro buy cancelled")
        mAnalytics?.forEach {
            it.proBuyCancelled()
        }
    }

    override fun proBuyAlreadyOwned() {
        Timber.tag(TAG).i("Pro buy already owned")
        mAnalytics?.forEach {
            it.proBuyAlreadyOwned()
        }
    }

    override fun proBuyRestore() {
        Timber.tag(TAG).i("Pro restore")
        mAnalytics?.forEach {
            it.proBuyRestore()
        }
    }

    override fun proManageSubscription() {
        Timber.tag(TAG).i("Pro manage subscription")
        mAnalytics?.forEach {
            it.proManageSubscription()
        }
    }

    override fun planCreated() {
        Timber.tag(TAG).i("Plan created")
        mAnalytics?.forEach {
            it.planCreated()
        }
    }

    override fun planModified() {
        Timber.tag(TAG).i("Plan modified")
        mAnalytics?.forEach {
            it.planModified()
        }
    }

    override fun planDeleted() {
        Timber.tag(TAG).i("Plan deleted")
        mAnalytics?.forEach {
            it.planDeleted()
        }
    }

    override fun planWeekAdded() {
        Timber.tag(TAG).i("Plan week added")
        mAnalytics?.forEach {
            it.planWeekAdded()
        }
    }

    override fun planWeekDeleted() {
        Timber.tag(TAG).i("Plan week deleted")
        mAnalytics?.forEach {
            it.planWeekDeleted()
        }
    }

    override fun planWeekDaySetRoutine() {
        Timber.tag(TAG).i("Plan week day set routine")
        mAnalytics?.forEach {
            it.planWeekDaySetRoutine()
        }
    }

    override fun planWeekDaySetRest() {
        Timber.tag(TAG).i("Plan week day set rest")
        mAnalytics?.forEach {
            it.planWeekDaySetRest()
        }
    }

    override fun planStarted() {
        Timber.tag(TAG).i("Plan started")
        mAnalytics?.forEach {
            it.planStarted()
        }
    }

    override fun planStopped() {
        Timber.tag(TAG).i("Plan stopped")
        mAnalytics?.forEach {
            it.planStopped()
        }
    }

    override fun planCompleted() {
        Timber.tag(TAG).i("Plan completed")
        mAnalytics?.forEach {
            it.planCompleted()
        }
    }

    override fun planDayComplete() {
        Timber.tag(TAG).i("Plan day complete")
        mAnalytics?.forEach {
            it.planDayComplete()
        }
    }

    override fun planCoverModified() {
        Timber.tag(TAG).i("Plan cover modified")
        mAnalytics?.forEach {
            it.planCoverModified()
        }
    }

    override fun integrationAccessRequested() {
        Timber.tag(TAG).i("Integration access requested")
        mAnalytics?.forEach {
            it.integrationAccessRequested()
        }
    }

    override fun integrationConnected() {
        Timber.tag(TAG).i("Integration connected")
        mAnalytics?.forEach {
            it.integrationConnected()
        }
    }

    override fun integrationDisconnected() {
        Timber.tag(TAG).i("Integration disconnected")
        mAnalytics?.forEach {
            it.integrationDisconnected()
        }
    }

    override fun integrationSyncRequested() {
        Timber.tag(TAG).i("Integration sync requested")
        mAnalytics?.forEach {
            it.integrationSyncRequested()
        }
    }

    override fun integrationUnsyncRequested() {
        Timber.tag(TAG).i("Integration unsync requested")
        mAnalytics?.forEach {
            it.integrationUnsyncRequested()
        }
    }

    override fun consentNewsletterGranted() {
        Timber.tag(TAG).i("Consent newsletter granted")
        mAnalytics?.forEach {
            it.consentNewsletterGranted()
        }
    }

    override fun consentNewsletterDenied() {
        Timber.tag(TAG).i("Consent newsletter denied")
        mAnalytics?.forEach {
            it.consentNewsletterDenied()
        }
    }
}