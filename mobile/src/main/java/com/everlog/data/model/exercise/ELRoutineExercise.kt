package com.everlog.data.model.exercise

import android.content.Context
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELFirestoreModel
import com.everlog.data.model.set.ELSet
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.append
import com.everlog.utils.device.DeviceUtils
import com.everlog.utils.format.StatsFormatUtils
import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

data class ELRoutineExercise(

        var uuid: String? = null,
        var exercise: ELExercise? = null,
        var sets: MutableList<ELSet> = ArrayList()

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun buildRoutineExercise(exercise: ELExercise): ELRoutineExercise {
            val routineExercise = ELRoutineExercise(UUID.randomUUID().toString(), exercise)
            // Add default sets to exercise
            val setCount = 1
            for (j in 0 until setCount) {
                routineExercise.sets.add(ELSet())
            }
            return routineExercise
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map["exercise"] = exercise?.asMap()
        map["sets"] = ELFirestoreModel.asMappedList(sets)
        return map
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ELRoutineExercise) {
            return false
        }
        return this.uuid.equals(other.uuid)
    }

    override fun hashCode(): Int {
        return this.uuid.hashCode()
    }

    fun modifyWeight(offset: Float, setIndex: Int) {
        val set: ELSet = sets[setIndex]
        if (set.getWeight() + offset <= 0) {
            if (!set.isWeightEntered() && offset > 0) {
                set.updateWeight(1f)
            } else {
                set.clearWeight()
            }
        } else {
            set.updateWeight(set.getWeight() + offset)
        }
    }

    fun modifyReps(offset: Int, setIndex: Int) {
        val set: ELSet = sets[setIndex]
        if (set.getReps() + offset <= 0) {
            if (!set.isRepsEntered() && offset > 0) {
                set.updateReps(1)
            } else {
                set.clearReps()
            }
        } else {
            set.updateReps(set.getReps() + offset)
        }
    }

    fun duplicateSet(set: ELSet): ELSet {
        val copy = SerializationUtils.clone(set)
        sets.add(copy)
        return copy
    }

    fun duplicatePreviousSet(): ELSet {
        return duplicateSet(sets[sets.size - 1])
    }

    fun addNewSet(): ELSet {
        val set = ELSet()
        sets.add(set)
        return set
    }

    fun getSetsWithData(): List<ELSet> {
        return sets.filter { !it.isWithoutData() }
    }

    fun getName(): String? {
        return exercise!!.name
    }

    fun getNotificationName(context: Context?): String? {
        val name = exercise!!.name
        val limit = if (DeviceUtils.isTablet(context)) name!!.length else 15
        return if (name!!.length > limit) {
            name.substring(0, limit) + "..."
        } else name
    }

    fun getCategory(): String? {
        return exercise!!.category
    }

    fun isLowerBody(): Boolean {
        return exercise!!.isLowerBody()
    }

    fun clearPerformedStats() {
        sets.forEach {
            it.clearPerformedStats()
        }
    }

    fun convertPerformedStats() {
        sets.forEach {
            it.convertPerformedStats()
        }
    }

    fun getTotalWeight(): Float {
        var weight = 0f
        sets.forEach {
            if (it.isWeightEntered()) {
                weight += if (it.isRepsEntered()) {
                    it.getReps() * it.getWeight()
                } else {
                    it.getWeight()
                }
            }
        }
        return weight
    }

    fun getTotalReps(): Int {
        var reps = 0
        sets.forEach {
            if (it.isRepsEntered()) {
                reps += it.getReps()
            }
        }
        return reps
    }

    fun getTotalTimeSeconds(): Int {
        var timeSeconds = 0
        sets.forEach {
            if (it.isTimeBased()) {
                timeSeconds += it.getTimeSeconds()
            }
        }
        return timeSeconds
    }

    fun getBestSet(compareWeight: Boolean): ELSet {
        var bestSet = if (sets.isNotEmpty()) sets.first() else null
        sets.forEach {
            if (it.isBetterThan(bestSet, compareWeight)) {
                bestSet = it
            }
        }
        return bestSet ?: ELSet()
    }

    fun getSummary(): String {
        val reps = getTotalReps()
        val weight = getTotalWeight()
        val timeSeconds = getTotalTimeSeconds()
        val separator = " • "
        var summary = ""
        if (weight != 0F) {
            // Include weight
            val weightSummary = String.format("%s %s", StatsFormatUtils.formatWeightStatsLabel(weight), SettingsManager.weightUnitAbbreviation())
            summary = summary.append(separator, weightSummary)
        }
        if (reps != 0) {
            // Include reps
            summary = summary.append(separator, ELApplication.getInstance().resources.getQuantityString(R.plurals.reps, reps, reps))
        }
        if (timeSeconds != 0) {
            // Include time
            val timeSummary = String.format("%s %s", StatsFormatUtils.formatTimeStatsLabel(TimeUnit.SECONDS.toMillis(timeSeconds.toLong()), "m:ss"), "m")
            summary = summary.append(separator, timeSummary)
        }
        return summary
    }

    // Exercise time

    /**
     * Returns -1 if the exercise times are mixed, 0 if nothing set and the value otherwise
     */
    fun getCommonExerciseTime(useTemplate: Boolean): Int {
        var time = 0
        var foundRepBasedSet = false
        sets.forEach {
            if (it.isTimeBased()) {
                if (time == 0) {
                    // Initial state
                    time = if (!useTemplate) it.getTimeSeconds() else it.getRequiredTimeSeconds()
                } else if (time != if (!useTemplate) it.getTimeSeconds() else it.getRequiredTimeSeconds()) {
                    // Safely return -1 if content is mixed
                    return -1
                }
            } else if (it.isRepBased()) {
                // Keep track of rep-based sets
                foundRepBasedSet = true
            }
        }
        if (time > 0 && foundRepBasedSet) {
            // Mixed set content detected
            return -1
        }
        return time
    }

    fun setCommonSettings(exerciseTimeSeconds: Int, useTemplate: Boolean) {
        sets.forEach {
            if (
                exerciseTimeSeconds > 0 // Time was set
                || (exerciseTimeSeconds == 0 && (if (!useTemplate) it.isTimeEntered() else it.isRequiredTimeEntered())) // Time was cleared
            ) {
                if (!useTemplate) it.updateTimeSeconds(exerciseTimeSeconds) else it.updateRequiredTimeSeconds(exerciseTimeSeconds)
            }
        }
    }
}