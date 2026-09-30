package com.everlog.managers

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
        if (!json.isNullOrEmpty()) {
            return Gson().fromJson(json, ELWorkout::class.java)
        }
        return null
    }

    /**
     * @return true if the given workout is the saved ongoing one, e.g. when it's being resumed
     */
    fun isOngoingWorkout(workout: ELWorkout): Boolean {
        return savedCopyOf(workout) != null
    }

    /**
     * @return the saved copy of the given workout if it's the ongoing one, with everything done since it started
     */
    fun savedCopyOf(workout: ELWorkout): ELWorkout? {
        return ongoingWorkout()?.takeIf { workout.uuid != null && it.uuid == workout.uuid }
    }

    fun setOngoingWorkout(workout: ELWorkout) {
        savePreference(Gson().toJson(workout), PreferenceKeys.ONGOING_WORKOUT.name)
    }
}