package com.everlog.managers.analytics

import android.os.Bundle
import com.everlog.data.model.set.ELSetType
import com.everlog.managers.appupdate.AppUpdateController
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.utils.device.DeviceUtils
import org.json.JSONObject
import java.io.Serializable

abstract class BaseAnalytic : Analytic {

    abstract fun logScreenName(eventName: String, screenName: String?)

    abstract fun logUserIdentify(eventName: String, userId: String?)

    abstract fun logUserLogout(eventName: String)

    abstract fun logEvent(name: String)

    abstract fun logEvent(name: String, data: Map<String, Any?>)

    internal var mAnalyticsEnabled = true

    // Test Lab can set its flag after the app process has started (seen on Play pre-launch
    // devices), so AnalyticsManager.initialize() alone misses those runs. Check again before
    // logging and turn analytics off for the rest of the run.
    protected fun canLog(): Boolean {
        if (mAnalyticsEnabled && DeviceUtils.isFirebaseTestLabRun()) {
            toggleAnalytics(false)
        }
        return mAnalyticsEnabled
    }

    protected fun buildBundleFromMap(map: Map<String, *>): Bundle {
        val bundle = Bundle()
        for (key in map.keys) {
            val value = map[key]
            if (value is Serializable) {
                bundle.putSerializable(key, value as? Serializable)
            }
        }
        return bundle
    }

    protected fun buildJsonFromMap(map: Map<String, *>): JSONObject {
        val json = JSONObject()
        for (key in map.keys) {
            val value = map[key]
            if (value is Serializable) {
                json.put(key, value)
            }
        }
        return json
    }

    override fun toggleAnalytics(enabled: Boolean) {
        mAnalyticsEnabled = enabled
    }

    override fun remoteConfigFetched() {
        logEvent(AnalyticsConstants.EVENT_REMOTE_CONFIG_FETCHED)
    }

    override fun notificationHomeShown(title: String?) {
        logEvent(AnalyticsConstants.EVENT_NOTIFICATION_HOME_SHOWN, notificationHomeParams(title))
    }

    override fun notificationHomeTapped(title: String?) {
        logEvent(AnalyticsConstants.EVENT_NOTIFICATION_HOME_TAPPED, notificationHomeParams(title))
    }

    override fun notificationHomeDismissed(title: String?) {
        logEvent(AnalyticsConstants.EVENT_NOTIFICATION_HOME_DISMISSED, notificationHomeParams(title))
    }

    private fun notificationHomeParams(title: String?): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_TITLE] = title
        return map
    }

    override fun appUsageReminderShown(attempt: Int, title: String?) {
        logEvent(AnalyticsConstants.EVENT_APP_USAGE_REMINDER_SHOWN, appUsageReminderParams(attempt, title))
    }

    override fun appUsageReminderOpened(attempt: Int, title: String?) {
        logEvent(AnalyticsConstants.EVENT_APP_USAGE_REMINDER_OPENED, appUsageReminderParams(attempt, title))
    }

    private fun appUsageReminderParams(attempt: Int, title: String?): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_ATTEMPT] = attempt
        map[AnalyticsConstants.PROPERTY_TITLE] = title
        return map
    }

    override fun ratePromptTriggered(source: String, promptNumber: Int) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        // Firebase only takes String, Long and Double values
        map[AnalyticsConstants.PROPERTY_PROMPT_NUMBER] = promptNumber.toLong()
        logEvent(AnalyticsConstants.EVENT_APP_RATE_PROMPT_TRIGGERED, map)
    }

    override fun screenName(screenName: String?) {
        logScreenName(AnalyticsConstants.EVENT_SCREEN_VIEW, screenName)
    }

    override fun userRegister(userId: String?, method: String) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_USER_ID] = userId
        map[AnalyticsConstants.PROPERTY_METHOD] = method
        logEvent(AnalyticsConstants.EVENT_USER_REGISTER, map)
    }

    override fun userLogin(userId: String?, method: String) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_METHOD] = method
        logEvent(AnalyticsConstants.EVENT_USER_LOGIN, map)
    }

    override fun userIdentify(userId: String?) {
        logUserIdentify(AnalyticsConstants.EVENT_USER_IDENTIFY, userId)
    }

    override fun userLogout() {
        logUserLogout(AnalyticsConstants.EVENT_USER_LOGOUT)
    }

    override fun routineCreated() {
        logEvent(AnalyticsConstants.EVENT_ROUTINE_CREATED)
    }

    override fun routineModified() {
        logEvent(AnalyticsConstants.EVENT_ROUTINE_MODIFIED)
    }

    override fun routineDeleted() {
        logEvent(AnalyticsConstants.EVENT_ROUTINE_DELETED)
    }

    override fun routineExerciseGroupAdded() {
        logEvent(AnalyticsConstants.EVENT_ROUTINE_EXERCISE_GROUP_ADDED)
    }

    override fun routineExerciseModified() {
        logEvent(AnalyticsConstants.EVENT_ROUTINE_EXERCISE_MODIFIED)
    }

    override fun setTypeSelected(type: ELSetType?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_TYPE] = type?.name
        logEvent(AnalyticsConstants.EVENT_SET_TYPE_SELECTED, map)
    }

    override fun setWeightModified(value: Float?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_WEIGHT_MODIFIED, map)
    }

    override fun setRequiredRepsModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_REQUIRED_REPS_MODIFIED, map)
    }

    override fun setRepsModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_REPS_MODIFIED, map)
    }

    override fun setRequiredTimeModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_REQUIRED_TIME_MODIFIED, map)
    }

    override fun setTimeModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_TIME_MODIFIED, map)
    }

    override fun setRestTimeModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SET_REST_TIME_MODIFIED, map)
    }

    override fun setAdded() {
        logEvent(AnalyticsConstants.EVENT_SET_ADDED)
    }

    override fun setDeleted() {
        logEvent(AnalyticsConstants.EVENT_SET_DELETED)
    }

    override fun setCompleted(source: String, type: ELSetType?, timed: Boolean) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        map[AnalyticsConstants.PROPERTY_SET_TYPE] = type?.name
        // Firebase params are strings or numbers
        map[AnalyticsConstants.PROPERTY_TIMED] = if (timed) 1 else 0
        logEvent(AnalyticsConstants.EVENT_SET_COMPLETED, map)
    }

    override fun setGroupCompleted(source: String, type: ELSetType?, exercises: Int) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        map[AnalyticsConstants.PROPERTY_SET_TYPE] = type?.name
        map[AnalyticsConstants.PROPERTY_EXERCISES] = exercises
        logEvent(AnalyticsConstants.EVENT_SET_GROUP_COMPLETED, map)
    }

    override fun setEdited(type: ELSetType?, timed: Boolean) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SET_TYPE] = type?.name
        map[AnalyticsConstants.PROPERTY_TIMED] = if (timed) 1 else 0
        logEvent(AnalyticsConstants.EVENT_SET_EDITED, map)
    }

    override fun statisticsRangeModified(rangeType: StatisticsHomeFragment.RangeType) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_TYPE] = rangeType
        logEvent(AnalyticsConstants.EVENT_STATISTICS_RANGE_MODIFIED, map)
    }

    override fun settingsMuscleGoalModified(value: SettingsManager.MuscleGoal?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value?.name
        logEvent(AnalyticsConstants.EVENT_SETTINGS_MUSCLE_GOAL_MODIFIED, map)
    }

    override fun settingsWeightModified(value: Float?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SETTINGS_WEIGHT_MODIFIED, map)
    }

    override fun settingsWeeklyGoalModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SETTINGS_WEEKLY_GOAL_MODIFIED, map)
    }

    override fun settingsRestTimeModified(value: Int?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SETTINGS_REST_TIME_MODIFIED, map)
    }

    override fun settingsWeightUnitModified(value: String?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SETTINGS_WEIGHT_UNIT_MODIFIED, map)
    }

    override fun settingsFirstWeekDayModified(value: String?) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        logEvent(AnalyticsConstants.EVENT_SETTINGS_FIRST_WEEK_DAY_MODIFIED, map)
    }

    override fun settingsKeepScreenOnModified(value: Boolean) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = if (value) 1 else 0
        logEvent(AnalyticsConstants.EVENT_SETTINGS_KEEP_SCREEN_ON_MODIFIED, map)
    }

    override fun homeAddFabTapped() {
        logEvent(AnalyticsConstants.EVENT_HOME_ADD_FAB_TAPPED)
    }

    override fun homeAddWeekEmptyStateTapped() {
        logEvent(AnalyticsConstants.EVENT_HOME_ADD_WEEK_EMPTY_STATE_TAPPED)
    }

    override fun appUpdatePromptShown(versionCode: Int) {
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_PROMPT_SHOWN, appUpdateParams(versionCode))
    }

    override fun appUpdateAccepted(versionCode: Int) {
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_ACCEPTED, appUpdateParams(versionCode))
    }

    override fun appUpdateDeclined(versionCode: Int) {
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_DECLINED, appUpdateParams(versionCode))
    }

    override fun appUpdateDownloaded() {
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_DOWNLOADED)
    }

    override fun appUpdateRestartTapped() {
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_RESTART_TAPPED)
    }

    override fun appUpdateFailed(failure: AppUpdateController.Failure, errorCode: Int) {
        val map = appUpdateParams(errorCode)
        map[AnalyticsConstants.PROPERTY_TYPE] = failure.name
        logEvent(AnalyticsConstants.EVENT_APP_UPDATE_FAILED, map)
    }

    private fun appUpdateParams(value: Int): MutableMap<String, Any?> {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_VALUE] = value
        return map
    }

    override fun workoutStarted(source: String) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        logEvent(AnalyticsConstants.EVENT_WORKOUT_STARTED, map)
    }

    override fun workoutStopped() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_STOPPED)
    }

    override fun workoutCompleted() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_COMPLETED)
    }

    override fun workoutDiscardPromptShown(source: String, setsCompleted: Int) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        map[AnalyticsConstants.PROPERTY_SETS_COMPLETED] = setsCompleted
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DISCARD_PROMPT_SHOWN, map)
    }

    override fun workoutDiscardPromptFinished() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DISCARD_PROMPT_FINISHED)
    }

    override fun workoutDiscardPromptCancelled(source: String) {
        val map = HashMap<String, Any?>()
        map[AnalyticsConstants.PROPERTY_SOURCE] = source
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DISCARD_PROMPT_CANCELLED, map)
    }

    override fun workoutNextExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_NEXT_EXERCISE)
    }

    override fun workoutPrevExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_PREV_EXERCISE)
    }

    override fun workoutRepsModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_REPS_MODIFIED)
    }

    override fun workoutTimeModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_TIME_MODIFIED)
    }

    override fun workoutWeightModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_WEIGHT_MODIFIED)
    }

    override fun workoutServiceNextExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_NEXT_EXERCISE)
    }

    override fun workoutServicePrevExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_PREV_EXERCISE)
    }

    override fun workoutServiceRepsModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_REPS_MODIFIED)
    }

    override fun workoutServiceWeightModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_WEIGHT_MODIFIED)
    }

    override fun workoutServiceWorkoutCompleted() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_WORKOUT_COMPLETED)
    }

    override fun workoutServiceTimerStartedExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_TIMER_STARTED_EXERCISE)
    }

    override fun workoutServiceTimerStoppedExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_TIMER_STOPPED_EXERCISE)
    }

    override fun workoutServiceTimerStoppedRest() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_SERVICE_TIMER_STOPPED_REST)
    }

    override fun workoutChangeMuscleGoal() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_CHANGE_MUSCLE_GOAL)
    }

    override fun workoutTimerStartedExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_TIMER_STARTED_EXERCISE)
    }

    override fun workoutTimerStoppedExercise() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_TIMER_STOPPED_EXERCISE)
    }

    override fun workoutTimerStartedRest() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_TIMER_STARTED_REST)
    }

    override fun workoutTimerStoppedRest() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_TIMER_STOPPED_REST)
    }

    override fun workoutDetailsNameModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_NAME_MODIFIED)
    }

    override fun workoutDetailsDateModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_DATE_MODIFIED)
    }

    override fun workoutDetailsNotesModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_NOTES_MODIFIED)
    }

    override fun workoutDetailsExercisesModified() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_EXERCISES_MODIFIED)
    }

    override fun workoutDetailsShared() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_SHARED)
    }

    override fun workoutDetailsSavedAsRoutine() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_SAVED_AS_ROUTINE)
    }

    override fun workoutDetailsDeleted() {
        logEvent(AnalyticsConstants.EVENT_WORKOUT_DETAILS_DELETED)
    }

    override fun exerciseCreated() {
        logEvent(AnalyticsConstants.EVENT_EXERCISE_CREATED)
    }

    override fun exerciseCreatedSuggestion() {
        logEvent(AnalyticsConstants.EVENT_EXERCISE_CREATED_SUGGESTION)
    }

    override fun exerciseModified() {
        logEvent(AnalyticsConstants.EVENT_EXERCISE_MODIFIED)
    }

    override fun exerciseDeleted() {
        logEvent(AnalyticsConstants.EVENT_EXERCISE_DELETED)
    }

    override fun proBuyMonth() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_MONTH)
    }

    override fun proBuyYear() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_YEAR)
    }

    override fun proBuyPurchased() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_PURCHASED)
    }

    override fun proBuyCancelled() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_CANCELLED)
    }

    override fun proBuyAlreadyOwned() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_ALREADY_OWNED)
    }

    override fun proBuyRestore() {
        logEvent(AnalyticsConstants.EVENT_PRO_BUY_RESTORE)
    }

    override fun proManageSubscription() {
        logEvent(AnalyticsConstants.EVENT_PRO_MANAGE_SUBSCRIPTION)
    }

    override fun planCreated() {
        logEvent(AnalyticsConstants.EVENT_PLAN_CREATED)
    }

    override fun planModified() {
        logEvent(AnalyticsConstants.EVENT_PLAN_MODIFIED)
    }

    override fun planDeleted() {
        logEvent(AnalyticsConstants.EVENT_PLAN_DELETED)
    }

    override fun planWeekAdded() {
        logEvent(AnalyticsConstants.EVENT_PLAN_WEEK_ADDED)
    }

    override fun planWeekDeleted() {
        logEvent(AnalyticsConstants.EVENT_PLAN_WEEK_DELETED)
    }

    override fun planWeekDaySetRoutine() {
        logEvent(AnalyticsConstants.EVENT_PLAN_WEEK_DAY_SET_ROUTINE)
    }

    override fun planWeekDaySetRest() {
        logEvent(AnalyticsConstants.EVENT_PLAN_WEEK_DAY_SET_REST)
    }

    override fun planStarted() {
        logEvent(AnalyticsConstants.EVENT_PLAN_STARTED)
    }

    override fun planStopped() {
        logEvent(AnalyticsConstants.EVENT_PLAN_STOPPED)
    }

    override fun planCompleted() {
        logEvent(AnalyticsConstants.EVENT_PLAN_COMPLETED)
    }

    override fun planDayComplete() {
        logEvent(AnalyticsConstants.EVENT_PLAN_DAY_COMPLETE)
    }

    override fun planCoverModified() {
        logEvent(AnalyticsConstants.EVENT_PLAN_COVER_MODIFIED)
    }

    override fun integrationAccessRequested() {
        logEvent(AnalyticsConstants.EVENT_INTEGRATION_ACCESS_REQUESTED)
    }

    override fun integrationConnected() {
        logEvent(AnalyticsConstants.EVENT_INTEGRATION_CONNECTED)
    }

    override fun integrationDisconnected() {
        logEvent(AnalyticsConstants.EVENT_INTEGRATION_DISCONNECTED)
    }

    override fun integrationSyncRequested() {
        logEvent(AnalyticsConstants.EVENT_INTEGRATION_SYNC_REQUESTED)
    }

    override fun integrationUnsyncRequested() {
        logEvent(AnalyticsConstants.EVENT_INTEGRATION_UNSYNC_REQUESTED)
    }

    override fun consentNewsletterGranted() {
        logEvent(AnalyticsConstants.EVENT_CONSENT_NEWSLETTER_GRANTED)
    }

    override fun consentNewsletterDenied() {
        logEvent(AnalyticsConstants.EVENT_CONSENT_NEWSLETTER_DENIED)
    }
}