package com.everlog.data.datastores.plans

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.data.store.InMemoryDatabase
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.data.datastores.routines.ELUserRoutinesInMemoryStore

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
