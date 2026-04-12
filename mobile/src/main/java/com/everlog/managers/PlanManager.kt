package com.everlog.managers

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELConstants
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanState
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.PreferencesManager
import com.google.gson.Gson

class PlanManager : PreferencesManager() {

    private enum class PreferenceKeys {
        ONGOING_PLAN,
        ONGOING_PLAN_STATE,
    }

    companion object {

        @JvmField
        val manager = PlanManager()
    }

    fun checkCompletedWorkout(workout: ELWorkout, performingFromPlan: Boolean) {
        // Check plan and mark plan day as complete
        if (hasOngoingPlan() && performingFromPlan) {
            val state = ongoingPlanState()
            state?.finishWorkout(workout)
            val plan = ongoingPlan()
            val day = plan?.getNextDay()
            day?.complete = true
            setOngoingPlan(plan)
            setOngoingState(state!!)
            AnalyticsManager.manager.planDayComplete()
        }
    }

    fun updateRoutineForPlan(context: Context, toSave: ELRoutine) {
        if (hasOngoingPlan()) {
            val plan = ongoingPlan()
            plan!!.editRoutine(toSave)
            setOngoingPlan(plan)
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ELConstants.BROADCAST_CURRENT_PLAN_CHANGED))
        }
    }

    fun deleteRoutineForPlan(context: Context, toSave: ELRoutine) {
        if (hasOngoingPlan()) {
            val plan = ongoingPlan()
            plan!!.deleteRoutine(toSave)
            setOngoingPlan(plan)
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ELConstants.BROADCAST_CURRENT_PLAN_CHANGED))
        }
    }

    fun isOngoing(plan: ELPlan?): Boolean {
        return hasOngoingPlan() && ongoingPlan()?.uuid.equals(plan?.uuid)
    }

    fun hasOngoingPlan(): Boolean {
        return ongoingPlan() != null && ongoingPlanState() != null
    }

    fun clearOngoingPlan() {
        val sharedPref = preferences
        val editor = sharedPref.edit()
        editor.remove(PreferenceKeys.ONGOING_PLAN.name)
        editor.remove(PreferenceKeys.ONGOING_PLAN_STATE.name)
        editor.apply()
    }

    fun ongoingPlan(): ELPlan? {
        val json = getPreference(PreferenceKeys.ONGOING_PLAN.name, "")
        if (!TextUtils.isEmpty(json)) {
            val gson = Gson()
            return gson.fromJson(json, ELPlan::class.java)
        }
        return null
    }

    fun ongoingPlanState(): ELPlanState? {
        val json = getPreference(PreferenceKeys.ONGOING_PLAN_STATE.name, "")
        if (!TextUtils.isEmpty(json)) {
            val gson = Gson()
            return gson.fromJson(json, ELPlanState::class.java)
        }
        return null
    }

    fun setOngoingPlan(plan: ELPlan?) {
        val gson = Gson()
        val json = gson.toJson(plan)
        savePreference(json, PreferenceKeys.ONGOING_PLAN.name)
        setOngoingState(ELPlanState())
    }

    fun setOngoingState(state: ELPlanState) {
        val gson = Gson()
        val json = gson.toJson(state)
        savePreference(json, PreferenceKeys.ONGOING_PLAN_STATE.name)
    }
}