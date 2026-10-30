package com.everlog.android.common.data.model.set

import com.everlog.android.common.data.model.ELFirestoreModel
import com.everlog.android.common.kotlin.UnitUtils.Companion.kgToLb
import com.everlog.android.common.kotlin.UnitUtils.Companion.lbToKg
import com.everlog.android.common.kotlin.WeightUnit
import com.google.firebase.firestore.Exclude
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
        // In kg, whatever the user's unit. A field, so Firestore and Gson map it by its name rather than through a getter
        @JvmField var weight: Float? = null,
        // Time
        private var requiredTimeSeconds: Int? = null,
        private var timeSeconds: Int? = null,

        // Aux

        var remainingTimeSeconds: Int? = null,
        // Deleted from one exercise in a superset or circuit's round during a workout, kept so the round's other
        // sets still line up with their exercises. Only the ongoing workout has these; finishing drops them.
        @get:Exclude @set:Exclude
        var removed: Boolean = false,
        // The values the Compose workout screen (TAS-455) filled in from history, weight in kg, until they're changed or
        // the set done: they show as suggestions. Only the ongoing workout has them.
        @get:Exclude @set:Exclude
        var suggestedWeight: Float? = null,
        @get:Exclude @set:Exclude
        var suggestedReps: Int? = null,
        @get:Exclude @set:Exclude
        var suggestedTimeSeconds: Int? = null

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
            compareWeight -> (weight ?: -1f) > (other.weight ?: -1f)
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
     * @return The weight in [unit], or -1 if it's not entered.
     */
    fun getWeight(unit: WeightUnit): Float {
        val kg = weight ?: return -1f
        return if (unit == WeightUnit.KILOGRAM) kg else kgToLb(kg)
    }

    /**
     * @param value A weight in [unit], stored in kg.
     */
    fun updateWeight(value: Float?, unit: WeightUnit) {
        weight = when {
            value == null -> value
            value <= 0 -> null
            unit == WeightUnit.KILOGRAM -> value
            else -> lbToKg(value)
        }
    }

    fun modifyWeight(offset: Float, unit: WeightUnit) {
        if (getWeight(unit) + offset <= 0) {
            if (!isWeightEntered() && offset > 0) {
                updateWeight(1f, unit)
            } else {
                clearWeight()
            }
        } else {
            updateWeight(getWeight(unit) + offset, unit)
        }
    }

    /**
     * @return The weight as stored, in kg whatever the user's unit, or null if it's not entered.
     */
    @Exclude
    fun getWeightKg(): Float? {
        return weight
    }

    fun isWeightEntered(): Boolean {
        return (weight ?: -1f) > 0
    }

    fun clearWeight() {
        weight = null
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
}
