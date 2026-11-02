package com.everlog.android.plan.data

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.persistence.PreferencesManager
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanState
import com.google.gson.Gson

class RealPlanProgress : PreferencesManager(), PlanProgress {

    private enum class PreferenceKeys {
        ONGOING_PLAN,
        ONGOING_PLAN_STATE,
    }

    override fun checkCompletedWorkout(workout: ELWorkout, performingFromPlan: Boolean): Boolean {
        if (hasOngoingPlan() && performingFromPlan) {
            val state = ongoingPlanState()
            state?.finishWorkout(workout)
            val plan = ongoingPlan()
            val day = plan?.getNextDay()
            day?.complete = true
            setOngoingPlan(plan)
            setOngoingState(state!!)
            return true
        }
        return false
    }

    override fun updateRoutineForPlan(context: Context, toSave: ELRoutine) {
        if (hasOngoingPlan()) {
            val plan = ongoingPlan()
            plan!!.editRoutine(toSave)
            setOngoingPlan(plan)
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(PlanBroadcasts.CURRENT_PLAN_CHANGED))
        }
    }

    override fun deleteRoutineForPlan(context: Context, toSave: ELRoutine) {
        if (hasOngoingPlan()) {
            val plan = ongoingPlan()
            plan!!.deleteRoutine(toSave)
            setOngoingPlan(plan)
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(PlanBroadcasts.CURRENT_PLAN_CHANGED))
        }
    }

    override fun isOngoing(plan: ELPlan?): Boolean {
        return hasOngoingPlan() && ongoingPlan()?.uuid.equals(plan?.uuid)
    }

    override fun hasOngoingPlan(): Boolean {
        return ongoingPlan() != null && ongoingPlanState() != null
    }

    override fun clearOngoingPlan() {
        val sharedPref = preferences
        val editor = sharedPref.edit()
        editor.remove(PreferenceKeys.ONGOING_PLAN.name)
        editor.remove(PreferenceKeys.ONGOING_PLAN_STATE.name)
        editor.apply()
    }

    override fun ongoingPlan(): ELPlan? {
        val json = getPreference(PreferenceKeys.ONGOING_PLAN.name, "")
        if (!json.isNullOrEmpty()) {
            val gson = Gson()
            return gson.fromJson(json, ELPlan::class.java)
        }
        return null
    }

    override fun ongoingPlanState(): ELPlanState? {
        val json = getPreference(PreferenceKeys.ONGOING_PLAN_STATE.name, "")
        if (!json.isNullOrEmpty()) {
            val gson = Gson()
            return gson.fromJson(json, ELPlanState::class.java)
        }
        return null
    }

    override fun setOngoingPlan(plan: ELPlan?) {
        val gson = Gson()
        val json = gson.toJson(plan)
        savePreference(json, PreferenceKeys.ONGOING_PLAN.name)
        setOngoingState(ELPlanState())
    }

    override fun setOngoingState(state: ELPlanState) {
        val gson = Gson()
        val json = gson.toJson(state)
        savePreference(json, PreferenceKeys.ONGOING_PLAN_STATE.name)
    }
}