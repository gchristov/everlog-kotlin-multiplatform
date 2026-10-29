package com.everlog.data.datastores.plans

import com.everlog.data.datastores.base.InMemoryDatabase
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.data.datastores.routines.ELUserRoutinesInMemoryStore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.plan.ELPlan

/**
 * [ELPlanDecorator] for the in-memory stores, which resolves a plan's routines from memory instead
 * of Firestore.
 */
class ELPlanInMemoryDecorator {

    fun decoratePlan(plan: ELPlan) {
        val routineUuids = plan.getRoutinesToResolve()
        if (routineUuids.isEmpty()) {
            return
        }
        val routineDecorator = ELRoutineDecorator()
        val routines = routineUuids.mapNotNull { uuid ->
            (InMemoryDatabase.get(ELUserRoutinesInMemoryStore.COLLECTION, uuid) as? ELRoutine)?.let { routine ->
                routineDecorator.decorate(routine)
                uuid to routine
            }
        }.toMap()
        // Like ELPlanDecorator, resolve even when routines are missing, which resets their days
        plan.resolveRoutines(routines)
    }
}
