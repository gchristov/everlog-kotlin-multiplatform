package com.everlog.data.model.exercise

import com.everlog.config.AppConfig
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELFirestoreModel
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.utils.NumberUtils
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

data class ELExerciseGroup (

        var uuid: String? = null,
        var type: String? = null,
        var exercises: MutableList<ELRoutineExercise> = ArrayList(),
        var restTimeSeconds: Int = AppConfig.configuration.defaultRestTimeSeconds

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun buildDefault(type: ELSetType): ELExerciseGroup {
            val group = ELExerciseGroup()
            group.uuid = UUID.randomUUID().toString()
            group.type = type.name
            return group
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map[ELConstants.FIELD_TYPE] = type
        map["exercises"] = ELFirestoreModel.asMappedList(exercises)
        map["restTimeSeconds"] = restTimeSeconds
        return map
    }

    fun getSetType(): ELSetType {
        return ELSetType.valueOf(type!!)
    }

    fun changeSetType(type: ELSetType) {
        var startIndex = type.maxAllowedExercises()
        if (startIndex > 2) {
            startIndex = -1
        }
        if (startIndex >= 0) {
            val toRemove: MutableList<ELRoutineExercise> = ArrayList()
            for (i in startIndex until exercises.size) {
                toRemove.add(exercises[i])
            }
            toRemove.forEach {
                exercises.remove(it)
            }
        }
        this.type = type.name
    }

    fun hasRestTime(): Boolean {
        return restTimeSeconds > 0
    }

    /**
     * Returns -1 if the exercise times are mixed, 0 if nothing set and the value otherwise
     */
    fun getCommonExerciseTime(useTemplate: Boolean): Int {
        var time: Int? = null
        exercises.forEach {
            if (time == null) {
                // Initial state
                time = it.getCommonExerciseTime(useTemplate)
            } else if (time != it.getCommonExerciseTime(useTemplate)) {
                // Safely return -1 if content is mixed
                return -1
            }
        }
        return time ?: 0
    }

    fun setCommonSettings(exerciseTimeSeconds: Int, useTemplate: Boolean) {
        exercises.forEach {
            it.setCommonSettings(exerciseTimeSeconds, useTemplate)
        }
    }

    fun isWithOnlyOneExercise(): Boolean {
        return exercises.size == 1
    }

    fun getTotalSetsCount(): Int {
        var sets = 0
        exercises.forEach {
            sets = max(sets, it.sets.size)
        }
        return sets
    }

    fun getExercisesForSetIndex(setIndex: Int): List<ELRoutineExercise> {
        return exercises.filter { setIndex >= 0 && setIndex < it.sets.size }
    }

    fun setIsComplete(setIndex: Int): Boolean {
        getExercisesForSetIndex(setIndex).forEach {
            if (!it.sets[setIndex].isComplete()) {
                return false
            }
        }
        return true
    }

    fun setComplete(setIndex: Int) {
        getExercisesForSetIndex(setIndex).forEach {
            val set = it.sets[setIndex]
            set.updateStartedDate(Date().time)
            set.updateCompletedDate(Date().time)
        }
    }

    fun setClearCompletedDate(setIndex: Int) {
        getExercisesForSetIndex(setIndex).forEach {
            it.sets[setIndex].clearCompletedDate()
        }
    }

    fun setDuplicate(setIndex: Int) {
        getExercisesForSetIndex(setIndex).forEach {
            val set = it.sets[setIndex]
            it.duplicateSet(set)
        }
    }

    fun setAdd() {
        exercises.forEach {
            val set = it.duplicatePreviousSet()
            set.clearCompletedDate()
            set.clearStartDate()
        }
    }

    fun setForExercise(exercise: ELRoutineExercise, setIndex: Int): ELSet {
        return exercise.sets[setIndex]
    }

    fun setUpdateWeightFromInput(exercise: ELRoutineExercise,
                                 setIndex: Int,
                                 input: String): ELSet {
        val value = NumberUtils.parseFloat(input)
        val set = exercise.sets[setIndex]
        if (input.isEmpty()) {
            set.clearWeight()
        } else {
            set.updateWeight(value)
        }
        AnalyticsManager.manager.setWeightModified(value)
        return set
    }

    fun setUpdateRepsFromInput(exercise: ELRoutineExercise,
                               setIndex: Int,
                               input: String,
                               useTemplate: Boolean): ELSet {
        val value = NumberUtils.parseFloat(input).toInt()
        val set = exercise.sets[setIndex]
        if (!useTemplate) {
            if (input.isEmpty()) {
                set.clearReps()
            } else {
                set.updateReps(value)
            }
        } else {
            if (input.isEmpty()) {
                set.clearRequiredReps()
            } else {
                set.updateRequiredReps(value)
            }
        }
        if (!useTemplate) AnalyticsManager.manager.setRepsModified(value) else AnalyticsManager.manager.setRequiredRepsModified(value)
        return set
    }

    fun setUpdateTimeFromInput(exercise: ELRoutineExercise,
                               setIndex: Int,
                               input: String,
                               useTemplate: Boolean): ELSet {
        val value = NumberUtils.parseFloat(input).toInt()
        val set = exercise.sets[setIndex]
        if (!useTemplate) {
            if (input.isEmpty()) {
                set.clearTime()
            } else {
                set.updateTimeSeconds(value)
                if (set.remainingTimeSeconds != null) {
                    set.remainingTimeSeconds = value
                }
            }
        } else {
            if (input.isEmpty()) {
                set.clearRequiredTime()
            } else {
                set.updateRequiredTimeSeconds(value)
            }
        }
        if (!useTemplate) AnalyticsManager.manager.setTimeModified(value) else AnalyticsManager.manager.setRequiredTimeModified(value)
        return set
    }

    /**
     * @return true if the group has no more sets
     */
    fun setDelete(setIndex: Int): Boolean {
        var groupEmpty = false
        getExercisesForSetIndex(setIndex).forEach {
            it.sets.removeAt(setIndex)
            if (it.sets.isEmpty()) {
                groupEmpty = true
            }
        }
        return groupEmpty
    }

    /**
     * @param setType if specified, checks the set type of this group against setType first, returning null if they don't match
     * @return list of routine exercises from this group that match
     */
    fun findExercise(exercise: ELExercise, setType: String?): List<ELRoutineExercise>? {
        var results: ArrayList<ELRoutineExercise>? = null
        if (setType == null || setType.equals(type, ignoreCase = true)) {
            // If a type is specified, make sure they match
            exercises.forEach {
                if (it.exercise?.uuid == exercise.uuid) {
                    if (results == null) results = ArrayList()
                    results?.add(it)
                }
            }
        }
        return results
    }

    /**
     * @return true if the exercise has been found and changed in this routine.
     */
    fun updateExercise(exercise: ELExercise): Boolean {
        var updated = false
        exercises.forEach {
            if (it.exercise?.uuid == exercise.uuid) {
                it.exercise = exercise
                updated = true
            }
        }
        return updated
    }

//    fun updateExercise(exercise: ELRoutineExercise, setTargetLevel: SetTargetLevel) {
//        for (i in exercises.indices) {
//            val groupExercise = exercises[i]
//            if (groupExercise.uuid == exercise.uuid) {
//                exercises[i] = exercise
//            } else if (setTargetLevel != SetTargetLevel.EXERCISE) {
//                // Copy over sets based on target level
//                val clonedSets: MutableList<ELSet> = ArrayList()
//                for (set in exercise.sets) {
//                    clonedSets.add(SerializationUtils.clone(set))
//                }
//                groupExercise.sets = clonedSets
//            }
//        }
//    }

    fun clearPerformedStats() {
        exercises.forEach {
            it.clearPerformedStats()
        }
    }

    fun convertPerformedStats() {
        exercises.forEach {
            it.convertPerformedStats()
        }
    }

    /**
     * Makes all exercises have the same number of sets as the exercise with the most sets, duplicating them all
     */
    fun ensureEqualSets() {
        val maxSetsPerExercise = getTotalSetsCount()
        exercises.forEach {
            while (it.sets.size < maxSetsPerExercise) {
                it.duplicatePreviousSet()
            }
        }
    }
}