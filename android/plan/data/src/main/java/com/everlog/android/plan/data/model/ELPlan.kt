package com.everlog.android.plan.data.model

import com.everlog.android.common.data.model.ELFirestoreModel
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.firebase.FirestoreFields
import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

data class ELPlan (

        var uuid: String? = null,
        var name: String? = null,
        var createdDate: Long = 0,
        var imageUrl: String? = null,
        var createdByUserId: String? = null,
        var privateToUser: Boolean = false,
        var weeks: ArrayList<ELPlanWeek> = ArrayList()

) : Serializable, ELFirestoreModel {

    companion object {

        const val MAX_WEEKS = 20

        fun newPlan(userId: String?): ELPlan {
            val plan = ELPlan()
            plan.setCreatedDateAsDate(Date())
            plan.uuid = UUID.randomUUID().toString()
            plan.createdByUserId = userId
            plan.privateToUser = true
            plan.addNewWeek()
            return plan
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[FirestoreFields.UUID] = uuid
        map["imageUrl"] = imageUrl
        map[FirestoreFields.CREATED_BY_USER_ID] = createdByUserId
        map["privateToUser"] = privateToUser
        val list: MutableList<Map<String, Any?>> = ArrayList()
        for (week in weeks) {
            list.add(week.asMap())
        }
        map["weeks"] = list
        map[FirestoreFields.NAME] = name
        map[FirestoreFields.CREATED_DATE] = createdDate
        return map
    }

    fun editRoutine(routine: ELRoutine) {
        for (week in weeks) {
            week.editRoutine(routine)
        }
    }

    fun deleteRoutine(routine: ELRoutine) {
        for (week in weeks) {
            week.deleteRoutine(routine)
        }
    }

    fun getNextDay(): ELPlanDay? {
        val currentDayIndex: Int = getDayIndex()
        val weekIndex = currentDayIndex / 7
        val dayIndex = currentDayIndex % 7
        if (weekIndex >= 0 && weekIndex < weeks.size) {
            val days = weeks[weekIndex].getDays()
            if (dayIndex >= 0 && dayIndex < days.size) {
                return days[dayIndex]
            }
        }
        return null
    }

    private fun canAddWeek(): Boolean {
        return weeks.size < MAX_WEEKS
    }

    fun resolveRoutines(routineMap: Map<String, ELRoutine>): Boolean {
        var success = true
        for (week in weeks) {
            if (!week.resolveRoutines(routineMap)) {
                success = false
            }
        }
        return success
    }

    fun getRoutinesToResolve(): Set<String> {
        val routinesToResolve: MutableSet<String> = HashSet()
        for (week in weeks) {
            routinesToResolve.addAll(week.getRoutinesToResolve())
        }
        return routinesToResolve
    }

    fun getDaysCount(): Int {
        return weeks.size * 7
    }

    fun getProgress(): Int {
        var completedDays = 0
        for (weekIndex in weeks.indices) {
            val days = weeks[weekIndex].getDays()
            for (dayIndex in days.indices) {
                val day = days[dayIndex]
                if (day.complete) {
                    completedDays++
                }
            }
        }
        return completedDays * 100 / getDaysCount()
    }

    fun getDayIndex(): Int {
        var count = 0
        for (weekIndex in weeks.indices) {
            val days = weeks[weekIndex].getDays()
            for (dayIndex in days.indices) {
                val day = days[dayIndex]
                if (day.complete) {
                    count++
                } else {
                    return count
                }
            }
        }
        return count
    }

    fun hasWeeksWithWorkouts(): Boolean {
        for (week in weeks) {
            if (week.isValid()) {
                return true
            }
        }
        return false
    }

    fun duplicateWeek(week: ELPlanWeek): ELPlanWeek? {
        if (!canAddWeek()) {
            return null
        }
        val copy = SerializationUtils.clone(week)
        weeks.add(copy)
        return copy
    }

    fun duplicatePreviousWeek(): ELPlanWeek? {
        return duplicateWeek(weeks[weeks.size - 1])
    }

    fun addNewWeek(): ELPlanWeek? {
        if (!canAddWeek()) {
            return null
        }
        val week = ELPlanWeek.newWeek()
        weeks.add(week)
        return week
    }

    fun removeWeek(index: Int) {
        if (index >= 0 && index < weeks.size) {
            weeks.removeAt(index)
        }
    }

    fun isEditable(userId: String?): Boolean {
        return createdByUserId == userId
    }

    private fun setCreatedDateAsDate(date: Date) {
        createdDate = date.time
    }
}