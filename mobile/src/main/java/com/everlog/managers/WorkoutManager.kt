package com.everlog.managers

import android.text.TextUtils
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.PreferencesManager
import com.google.gson.Gson

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
}