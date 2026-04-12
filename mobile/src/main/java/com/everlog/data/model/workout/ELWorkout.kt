package com.everlog.data.model.workout

import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELFirestoreModel
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.utils.*
import com.everlog.utils.format.FormatUtils
import com.everlog.utils.format.StatsFormatUtils
import org.apache.commons.lang3.SerializationUtils
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

data class ELWorkout(

        var uuid: String? = null,
        var routine: ELRoutine? = null,
        var createdDate: Long = 0,
        var completedDate: Long = 0,
        var fromRoutine: Boolean = false,
        var note: String? = null

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun getWorkoutFromRoutine(routine: ELRoutine, fromRoutine: Boolean): ELWorkout {
            val workout = ELWorkout()
            workout.uuid = UUID.randomUUID().toString()
            workout.setCreatedDateAsDate(Date())
            workout.fromRoutine = fromRoutine
            val clone = SerializationUtils.clone(routine)
            clone.clearPerformedStats()
            workout.routine = clone
            return workout
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map["completedDate"] = completedDate
        map["routine"] = routine?.asMap()
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        map["fromRoutine"] = fromRoutine
        map["note"] = note
        return map
    }

    fun getRoutineFromWorkout(): ELRoutine {
        routine?.convertPerformedStats()
        return routine!!
    }

    /**
     * @return true if the exercise has been found and changed in this routine.
     */
    fun updateExercise(exercise: ELExercise): Boolean {
        return routine?.updateExercise(exercise) ?: false
    }

    fun getCreatedDateAsDate(): Date {
        return Date(createdDate)
    }

    fun setCreatedDateAsDate(createdDate: Date) {
        this.createdDate = createdDate.time
    }

    fun getCompletedDateAsDate(): Date {
        return Date(completedDate)
    }

    fun setCompletedDateAsDate(completedDate: Date) {
        val duration = getDurationMillis()
        this.completedDate = completedDate.time
        if (duration > 0) {
            // Offset new created date based on original duration
            this.createdDate = this.completedDate - duration
        }
    }

    fun getName(): String? {
        return routine?.name
    }

    fun setName(name: String) {
        routine?.name = name
    }

    fun getExerciseGroups(): List<ELExerciseGroup> {
        return routine?.exerciseGroups ?: ArrayList()
    }

    fun setExerciseGroups(groups: MutableList<ELExerciseGroup>) {
        routine?.exerciseGroups = groups
    }

    fun getTotalExercises(): Int {
        return routine?.getTotalExercises() ?: 0
    }

    fun hasExercises(): Boolean {
        return getExerciseGroups().isNotEmpty()
    }

    fun getDurationMillis(): Long {
        return completedDate - createdDate
    }

    fun getOngoingDuration(): String {
        val durationMillis = Date().time - createdDate
        return FormatUtils.formatDurationShort(durationMillis, "mm:ss")
    }

    fun hasWeight(): Boolean {
        return getTotalWeight() > 0
    }

    fun getCategoryCounts(): Map<String, Int> {
        val categories: MutableMap<String, Int> = HashMap()
        getExerciseGroups().forEach { group ->
            group.exercises.forEach { exercise ->
                var count = categories[exercise.getCategory()]
                if (count == null) {
                    count = 0
                }
                count++
                categories[exercise.getCategory()!!] = count
            }
        }
        return categories
    }

    fun getTotalWeight(): Float {
        var weight = 0f
        getExerciseGroups().forEach { group ->
            group.exercises.forEach { exercise ->
                weight += exercise.getTotalWeight()
            }
        }
        return weight
    }

    fun getMaxWeight(): Float {
        var weight = 0f
        getExerciseGroups().forEach { group ->
            group.exercises.forEach { exercise ->
                weight = max(weight, exercise.getBestSet(true).getWeight())
            }
        }
        return weight
    }

    fun getTotalSets(): Int {
        var sets = 0
        getExerciseGroups().forEach { group ->
            group.exercises.forEach { exercise ->
                sets += exercise.sets.size
            }
        }
        return sets
    }

    fun getSummary(): String {
        val separator = " • "
        var summary = ""
        // Include duration
        val durationSummary = String.format("%s %s", StatsFormatUtils.formatTimeStatsLabel(getDurationMillis()), ELApplication.getInstance().getString(R.string.hour))
        summary = summary.append(separator, durationSummary)
        if (hasWeight()) {
            // Include weight
            val weightSummary = String.format("%s %s", StatsFormatUtils.formatWeightStatsLabel(getTotalWeight()), SettingsManager.weightUnitAbbreviation())
            summary = summary.append(separator, weightSummary)
        }
        return summary
    }

    fun findExercise(exercise: ELExercise): List<ELRoutineExercise>? {
        return findExercise(exercise, null)
    }

    private fun findExercise(exercise: ELExercise, setType: String?): List<ELRoutineExercise>? {
        var results: ArrayList<ELRoutineExercise>? = null
        getExerciseGroups().forEach {
            val foundExercises = it.findExercise(exercise, setType)
            if (foundExercises != null) {
                if (results == null) results = ArrayList()
                results?.addAll(foundExercises)
            }
        }
        return results
    }

    // Dates

    private fun isToday(): Boolean {
        return Date().isSameDay(completedDate)
    }

    private fun isThisWeek(): Boolean {
        return Date().isSameWeek(completedDate)
    }

    private fun isThisMonth(): Boolean {
        return Date().isSameMonth(completedDate)
    }

    private fun isThisYear(): Boolean {
        return Date().isSameYear(completedDate)
    }

    fun inRange(range: StatisticsHomeFragment.RangeType): Boolean {
        return when (range) {
            StatisticsHomeFragment.RangeType.TODAY -> isToday()
            StatisticsHomeFragment.RangeType.WEEK -> isThisWeek()
            StatisticsHomeFragment.RangeType.MONTH -> isThisMonth()
            StatisticsHomeFragment.RangeType.YEAR -> isThisYear()
            else -> true
        }
    }

    fun getDay(): Int {
        val now = Instant.ofEpochMilli(completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
        return now.dayOfMonth
    }

    fun getMonth(): Int {
        val now = Instant.ofEpochMilli(completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
        return now.monthValue
    }

    fun getYear(): Int {
        val now = Instant.ofEpochMilli(completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
        return now.year
    }

    fun getCompletedDateWithNoHourTimestamp(): Long {
        return getCompletedDateAsDate().toDayOfMonthWithNoHourTimestamp()
    }

    fun getCompletedDateWithMonthOnlyTimestamp(): Long {
        return getCompletedDateAsDate().toDayOfMonthWithMonthOnlyTimestamp()
    }

    fun getCompletedDateWithYearOnlyTimestamp(): Long {
        return getCompletedDateAsDate().toDayOfMonthWithYearOnlyTimestamp()
    }

    // Ongoing state

    fun updateRestTime(timeSeconds: Int) {
        getExerciseGroups().forEach {
            it.restTimeSeconds = timeSeconds
        }
    }

    fun getNextIncompleteState(allowSkip: Boolean? = true): ELWorkoutState? {
        if (hasExercises()) {
            // Start from beginning
            getExerciseGroups().forEachIndexed { groupIndex, group ->
                val sets = group.getTotalSetsCount()
                for (setIndex in 0 until sets) {
                    val exercisesForSetIndex = group.getExercisesForSetIndex(setIndex)
                    exercisesForSetIndex.forEachIndexed { exerciseIndex, exercise ->
                        if (!exercise.sets[setIndex].isComplete()
                                || (exercise.sets[setIndex].isWithoutData() && allowSkip == false)) {
                            val state = ELWorkoutState()
                            state.groupIndex = groupIndex
                            state.setIndex = setIndex
                            state.exerciseIndex = exerciseIndex
                            state.exercisesInGroup = exercisesForSetIndex.size
                            return state
                        }
                    }
                }
            }
        }
        return null
    }

    // Set prefill

    fun prefillRequiredMetrics() {
        // Make sure we have set the default values in case users do not manually set reps.
        getExerciseGroups().forEach { group ->
            group.exercises.forEach { exercise ->
                exercise.sets.forEach { set ->
                    if (!set.isRepsEntered() && set.isRequiredRepsEntered()) {
                        set.updateReps(set.getRequiredReps())
                    }
                    if (!set.isTimeEntered() && set.isRequiredTimeEntered()) {
                        set.updateTimeSeconds(set.getRequiredTimeSeconds())
                    }
                    // Clear this as we want it to be empty every time
                    set.remainingTimeSeconds = null
                }
            }
        }
    }
}