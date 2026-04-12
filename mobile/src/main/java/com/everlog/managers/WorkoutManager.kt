package com.everlog.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.AlarmManagerCompat
import com.everlog.application.ELApplication
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.managers.preferences.PreferencesManager
import com.everlog.receivers.WorkoutTimerReceiver
import com.google.gson.Gson
import timber.log.Timber
import java.util.*
import kotlin.random.Random

class WorkoutManager : PreferencesManager() {

    private enum class PreferenceKeys {
        ONGOING_WORKOUT
    }

    companion object {

        private const val TAG = "WorkoutManager"

        @JvmField
        val manager = WorkoutManager()
    }

    fun hasOngoingWorkout(): Boolean {
        return ongoingWorkout() != null
    }

    fun clearOngoingWorkout() {
        val editor = preferences.edit()
        editor.remove(PreferenceKeys.ONGOING_WORKOUT.name)
        editor.apply()
    }

    fun ongoingWorkout(): ELWorkout? {
        val json = getPreference(PreferenceKeys.ONGOING_WORKOUT.name, "")
        if (!TextUtils.isEmpty(json)) {
            return Gson().fromJson(json, ELWorkout::class.java)
        }
        return null
    }

    fun setOngoingWorkout(workout: ELWorkout) {
        savePreference(Gson().toJson(workout), PreferenceKeys.ONGOING_WORKOUT.name)
    }

    fun setOngoingTimer(active: Boolean, secondsFromNow: Int? = -1) {
        if (active) {
            scheduleWorkoutTimer(secondsFromNow!!)
        } else {
            cancelWorkoutTimer()
        }
    }

    // Workout timer

    private fun scheduleWorkoutTimer(secondsFromNow: Int) {
        cancelWorkoutTimer()
        val cal = Calendar.getInstance()
        cal.add(Calendar.SECOND, secondsFromNow);
        val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildAlarmIntent()
        AlarmManagerCompat.setAlarmClock(alarmManager, cal.timeInMillis, intent, intent)
        Timber.tag(TAG).i("Scheduled workout timer")
    }

    private fun cancelWorkoutTimer() {
        val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmIntent())
        Timber.tag(TAG).i("Cancelled pending workout timer")
    }

    private fun buildAlarmIntent(): PendingIntent {
        val notificationIntent = Intent(ELApplication.getInstance(), WorkoutTimerReceiver::class.java)
        return PendingIntent.getBroadcast(ELApplication.getInstance(), Random.nextInt(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)
    }
}