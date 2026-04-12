package com.everlog.data.model

import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.constants.ELConstants
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.utils.append
import com.everlog.utils.timeOfDayMessage
import java.io.Serializable
import java.util.*

data class ELRoutine(

        var uuid: String? = null,
        var name: String? = null,
        var exerciseGroups: MutableList<ELExerciseGroup> = ArrayList(),
        var createdDate: Long = 0

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun buildEmptyWorkout(): ELRoutine {
            val routine = ELRoutine()
            routine.setCreatedDateAsDate(Date())
            routine.uuid = UUID.randomUUID().toString()
            routine.name = Date().timeOfDayMessage() + " Workout"
            return routine
        }

        @JvmStatic
        fun buildNewRoutine(userId: String): ELRoutine {
            val routine = ELRoutine()
            routine.setCreatedDateAsDate(Date())
            routine.uuid = UUID.randomUUID().toString()
            routine.name = "My Template"
            return routine
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        val list: MutableList<Map<String, Any?>> = ArrayList()
        for (setGroup in exerciseGroups) {
            list.add(setGroup.asMap())
        }
        map["exerciseGroups"] = list
        map[ELConstants.FIELD_NAME] = name
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        return map
    }

    fun getRestTimeSeconds(): Int {
        if (canBePerformed()) {
            return exerciseGroups.first().restTimeSeconds
        }
        return -1
    }

    fun getCreatedDateAsDate(): Date? {
        return Date(createdDate)
    }

    fun setCreatedDateAsDate(createdDate: Date) {
        this.createdDate = createdDate.time
    }

    fun getTotalSets(): Int {
        var count = 0
        exerciseGroups.forEach {
            count += it.getTotalSetsCount()
        }
        return count
    }

    fun getTotalExercises(): Int {
        var count = 0
        exerciseGroups.forEach {
            count += it.exercises.size
        }
        return count
    }

    fun canBePerformed(): Boolean {
        return getTotalExercises() > 0
    }

    fun getSummary(): String {
        val separator = " • "
        var summary = ""
        // Include exercises
        val exercisesCount = getTotalExercises()
        summary = summary.append(separator, ELApplication.getInstance().resources.getQuantityString(R.plurals.exercises, exercisesCount, exercisesCount))
        // Include sets
        val setsCount = getTotalSets()
        summary = summary.append(separator, ELApplication.getInstance().resources.getQuantityString(R.plurals.sets, setsCount, setsCount))
        return summary
    }

    /**
     * @return true if the exercise has been found and changed in this routine.
     */
    fun updateExercise(exercise: ELExercise?): Boolean {
        var updated = false
        exerciseGroups.forEach {
            if (it.updateExercise(exercise!!)) {
                updated = true
            }
        }
        return updated
    }

    fun clearPerformedStats() {
        exerciseGroups.forEach {
            it.clearPerformedStats()
        }
    }

    fun convertPerformedStats() {
        exerciseGroups.forEach {
            it.convertPerformedStats()
        }
    }
}