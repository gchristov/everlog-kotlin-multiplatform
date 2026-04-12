package com.everlog.data.model.set

import android.content.Context
import android.text.TextUtils
import com.everlog.R
import com.everlog.data.model.ELFirestoreModel
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.UnitUtils.Companion.kgToLb
import com.everlog.utils.UnitUtils.Companion.lbToKg
import com.everlog.utils.format.FormatUtils
import java.io.Serializable
import java.util.*

data class ELSet (

        // Dates
        private var startedDate: Long? = null,
        private var completedDate: Long? = null,
        // Reps
        private var requiredReps: Int? = null,
        private var reps: Int? = null,
        // Weight
        private var weight: Float? = null, // KG
        // Time
        private var requiredTimeSeconds: Int? = null,
        private var timeSeconds: Int? = null,

        // Aux

        var remainingTimeSeconds: Int? = null

) : Serializable, ELFirestoreModel {

    override fun documentId(): String {
        return UUID.randomUUID().toString()
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        // Dates
        map["startedDate"] = startedDate
        map["completedDate"] = completedDate
        // Reps
        map["requiredReps"] = requiredReps
        map["reps"] = reps
        // Weight
        map["weight"] = weight
        // Time
        map["requiredTimeSeconds"] = requiredTimeSeconds
        map["timeSeconds"] = timeSeconds
        return map
    }

    fun isBetterThan(other: ELSet?, compareWeight: Boolean): Boolean {
        if (other == null) {
            return false
        }
        return when {
            compareWeight -> getWeight() > other.getWeight()
            else -> getReps() > other.getReps()
        }
    }

    fun isRepBased(): Boolean {
        return isRequiredRepsEntered() || isRepsEntered()
    }

    fun isTimeBased(): Boolean {
        return isRequiredTimeEntered() || isTimeEntered()
    }

    fun isWithoutData(): Boolean {
        return !isRepsEntered() && !isTimeEntered()
    }

    /**
     * Returns true if the set is marked as "complete" during a workout (essentially has a complete date)
     */
    fun isComplete(): Boolean {
        return getCompletedDate() > 0
    }

    fun isEmpty(): Boolean {
        return !isRepBased() && !isTimeBased()
    }

    fun canShowRepOptions(): Boolean {
        // Reps and time are separate. Show reps by default
        return isEmpty() || isRepBased()
    }

    fun canShowTimeOptions(): Boolean {
        // Reps and time are separate
        return !isEmpty() && isTimeBased()
    }

    // Stats

    fun clearPerformedStats() {
        clearStartDate()
        clearCompletedDate()
        clearWeight()
        clearReps()
        clearTime()
    }

    fun convertPerformedStats() {
        if (isRepsEntered()) {
            updateRequiredReps(getReps())
        }
        if (isTimeEntered()) {
            updateRequiredTimeSeconds(getTimeSeconds())
        }
        clearPerformedStats()
    }

    // Weight

    /**
     * @return Calculated weight based on user preferences.
     */
    fun getWeight(): Float {
        if (weight != null) {
            return if (SettingsManager.manager.weightUnit() == SettingsManager.WeightUnit.KILOGRAM) {
                weight!!
            } else kgToLb(weight!!)
        }
        return -1f
    }

    /**
     * @param value Converts this value to the current weight unit and sets it.
     */
    fun updateWeight(value: Float?) {
        weight = when {
            value == null -> value
            value <= 0 -> null
            SettingsManager.manager.weightUnit() == SettingsManager.WeightUnit.KILOGRAM -> value
            else -> lbToKg(value)
        }
    }

    fun modifyWeight(offset: Float) {
        if (getWeight() + offset <= 0) {
            if (!isWeightEntered() && offset > 0) {
                updateWeight(1f)
            } else {
                clearWeight()
            }
        } else {
            updateWeight(getWeight() + offset)
        }
    }

    fun isWeightEntered(): Boolean {
        return getWeight() > 0
    }

    fun clearWeight() {
        updateWeight(null)
    }

    // Reps

    fun getRequiredReps(): Int {
        return if (requiredReps != null) {
            requiredReps!!
        } else {
            0
        }
    }

    fun getReps(): Int {
        return if (reps != null) {
            reps!!
        } else {
            -1
        }
    }

    fun updateRequiredReps(value: Int?) {
        requiredReps = when {
            value == null -> value
            value <= 0 -> null
            else -> {
                // Reps and time are separate
                clearRequiredTime()
                value
            }
        }
    }

    fun updateReps(value: Int?) {
        reps = when {
            value == null -> value
            value <= 0 -> null
            else -> {
                // Reps and time are separate
                remainingTimeSeconds = null
                clearTime()
                clearRequiredTime()
                value
            }
        }
    }

    fun modifyReps(offset: Int) {
        if (getReps() + offset <= 0) {
            if (!isRepsEntered() && offset > 0) {
                updateReps(1)
            } else {
                clearReps()
            }
        } else {
            updateReps(getReps() + offset)
        }
    }

    fun isRequiredRepsEntered(): Boolean {
        return getRequiredReps() > 0
    }

    fun isRepsEntered(): Boolean {
        return getReps() > 0
    }

    fun clearRequiredReps() {
        updateRequiredReps(null)
    }

    fun clearReps() {
        updateReps(null)
    }

    // Time

    fun getRequiredTimeSeconds(): Int {
        return if (requiredTimeSeconds != null) {
            requiredTimeSeconds!!
        } else {
            0
        }
    }

    fun getTimeSeconds(): Int {
        return if (timeSeconds != null) {
            timeSeconds!!
        } else {
            0
        }
    }

    fun updateRequiredTimeSeconds(value: Int?) {
        requiredTimeSeconds = when {
            value == null -> value
            value <= 0 -> null
            else -> {
                // Reps and time are separate
                clearRequiredReps()
                value
            }
        }
    }

    fun updateTimeSeconds(value: Int?) {
        timeSeconds = when {
            value == null -> value
            value <= 0 -> null
            else -> {
                // Reps and time are separate
                clearReps()
                clearRequiredReps()
                value
            }
        }
    }

    fun isRequiredTimeEntered(): Boolean {
        return requiredTimeSeconds != null
    }

    fun isTimeEntered(): Boolean {
        return timeSeconds != null
    }

    fun clearRequiredTime() {
        updateRequiredTimeSeconds(null)
    }

    fun clearTime() {
        updateTimeSeconds(null)
    }

    // Dates

    // Do NOT make this private, otherwise the Firebase model converter doesn't see it
    fun getStartedDate(): Long {
        return if (startedDate != null) {
            startedDate!!
        } else {
            0
        }
    }

    // Do NOT make this private, otherwise the Firebase model converter doesn't see it
    fun getCompletedDate(): Long {
        return if (completedDate != null) {
            completedDate!!
        } else {
            0
        }
    }

    fun updateStartedDate(value: Long?) {
        this.startedDate = value
    }

    fun updateCompletedDate(value: Long?) {
        this.completedDate = value
    }

    fun clearStartDate() {
        updateStartedDate(null)
    }

    fun clearCompletedDate() {
        updateCompletedDate(null)
    }

    // Summary

    fun getOngoingWorkoutNotificationSummary(context: Context?,
                                             setNumber: Int,
                                             setType: String): String {
        val type = ArrayResourceTypeUtils.withSetTypes().getTitle(setType, setType.lowercase().capitalize() + " Set")
        val builder = StringBuilder()
        builder.append("$type $setNumber")
        val setSummary = getExerciseSetSummary(context, true)
        if (!TextUtils.isEmpty(setSummary)) {
            builder.append(" (")
            builder.append(setSummary)
            builder.append(")")
        }
        return builder.toString()
    }

    fun getWorkoutDetailsSummary(context: Context?): String? {
        if (isWithoutData()) {
            return "Skipped"
        }
        return getExerciseSetSummary(context, false)
    }

    fun getExerciseSetSummary(context: Context?, useTemplate: Boolean): String? {
        return when {
            isRepBased() -> {
                val finalReps: Int = if (isRepsEntered() || !useTemplate) getReps() else getRequiredReps()
                val hasReps = finalReps > 0
                when {
                    hasReps -> {
                        if (isWeightEntered()) {
                            "$finalReps x ${formatWeight()}"
                        } else {
                            formatReps(context, finalReps)
                        }
                    }
                    else -> null
                }
            }
            isTimeBased() -> {
                val finalTimeSeconds: Int = if (isTimeEntered() || !useTemplate) getTimeSeconds() else getRequiredTimeSeconds()
                val hasTime = finalTimeSeconds > 0
                when {
                    hasTime -> {
                        if (isWeightEntered()) {
                            "${formatTime(finalTimeSeconds)} • ${formatWeight()}"
                        } else {
                            formatTime(finalTimeSeconds)
                        }
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun formatReps(context: Context?, reps: Int): String? {
        return context?.resources?.getQuantityString(R.plurals.reps, reps, reps)
    }

    private fun formatWeight(): String? {
        return "${FormatUtils.formatDecimal(getWeight())} ${SettingsManager.weightUnitAbbreviation()}"
    }

    private fun formatTime(timeSeconds: Int): String? {
        return FormatUtils.formatSetTime(timeSeconds)
    }
}
