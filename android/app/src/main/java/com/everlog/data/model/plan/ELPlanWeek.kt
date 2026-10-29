package com.everlog.data.model.plan

import com.everlog.config.AppConfig
import com.everlog.data.model.ELFirestoreModel
import com.everlog.data.model.ELRoutine
import com.everlog.managers.auth.LocalUserManager
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

data class ELPlanWeek (

        private var days: ArrayList<ELPlanDay> = ArrayList(),

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun newWeek(): ELPlanWeek {
            val week = ELPlanWeek()
            // Add days
            val days = ArrayList<ELPlanDay>()
            for (i in 0..6) {
                days.add(ELPlanDay())
            }
            week.setDays(days)
            return week
        }
    }

    fun setDays(days: ArrayList<ELPlanDay>) {
        this.days = days
        proLock()
    }

    fun getDays(): ArrayList<ELPlanDay> {
        return days
    }

    override fun documentId(): String {
        return UUID.randomUUID().toString()
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        val list: MutableList<Map<String, Any?>> = ArrayList()
        for (day in days) {
            list.add(day.asMap())
        }
        map["days"] = list
        return map
    }

    fun editRoutine(routine: ELRoutine) {
        for (day in days) {
            day.editRoutine(routine)
        }
    }

    fun deleteRoutine(routine: ELRoutine) {
        for (day in days) {
            day.deleteRoutine(routine)
        }
    }

    fun resolveRoutines(routineMap: Map<String, ELRoutine>): Boolean {
        var success = true
        for (day in days) {
            if (!day.resolveRoutines(routineMap)) {
                success = false
            }
        }
        return success
    }

    fun getRoutinesToResolve(): Set<String> {
        val routinesToResolve: MutableSet<String> = HashSet()
        for ((routineUuid) in days) {
            if (routineUuid != null) {
                routinesToResolve.add(routineUuid)
            }
        }
        return routinesToResolve
    }

    fun isValid(): Boolean {
        return getTotalWorkouts() > 0
    }

    fun getTotalWorkouts(): Int {
        var workouts = 0
        for (day in days) {
            if (day.getRoutine() != null) {
                workouts++
            }
        }
        return workouts
    }

    private fun proLock() {
        if (days.isNotEmpty()) {
            val dayLimit = AppConfig.configuration.maxPlanWeekDaysFree
            days.subList(0, dayLimit).forEach { it.proLocked = false }
            days.subList(dayLimit, days.size).forEach { it.proLocked = LocalUserManager.getUser()?.isPro() == false }
        }
    }
}