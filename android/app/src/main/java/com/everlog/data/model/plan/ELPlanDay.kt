package com.everlog.data.model.plan

import com.everlog.data.model.ELFirestoreModel
import com.everlog.data.model.ELRoutine
import java.io.Serializable
import java.util.*

data class ELPlanDay (

        var routineUuid: String? = null,
        private var rest: Boolean = false,

        // Aux

        private var routine: ELRoutine? = null,
        var complete: Boolean = false,
        var next: Boolean = false,
        var proLocked: Boolean = false,

) : Serializable, ELFirestoreModel {

    override fun documentId(): String {
        return UUID.randomUUID().toString()
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map["rest"] = rest
        map["routineUuid"] = routineUuid
        return map
    }

    fun getRoutine(): ELRoutine? {
        return routine
    }

    fun setRoutine(routine: ELRoutine?) {
        this.routine = routine
        if (routine != null) {
            routineUuid = routine.uuid
            rest = false
            complete = false
            next = false
        } else {
            routineUuid = null
        }
    }

    fun getRest(): Boolean {
        return rest
    }

    fun setRest(rest: Boolean) {
        this.rest = rest
        if (rest) {
            routine = null
            routineUuid = null
        }
    }

    fun editRoutine(routine: ELRoutine) {
        if (routineUuid != null && routineUuid == routine.uuid) {
            // Don't use setRoutine here as it will remove the complete state
            this.routine = routine
        }
    }

    fun deleteRoutine(routine: ELRoutine) {
        if (routineUuid != null && routineUuid == routine.uuid) {
            setRoutine(null)
        }
    }

    fun resolveRoutines(routineMap: Map<String, ELRoutine>): Boolean {
        if (routineUuid != null) {
            routine = routineMap[routineUuid!!]
            if (routine == null) {
                // If we cannot resolve the routine, clear this day
                reset()
            }
//            return routine != null;
        }
        return true
    }

    fun isEmpty(): Boolean {
        return routine == null && !rest
    }

    private fun reset() {
        rest = false
        routine = null
        routineUuid = null
        complete = false
        next = false
    }
}