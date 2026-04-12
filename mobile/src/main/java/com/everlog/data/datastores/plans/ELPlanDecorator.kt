package com.everlog.data.datastores.plans

import com.everlog.data.model.ELRoutine
import com.everlog.data.model.plan.ELPlan
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.utils.Utils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Source
import timber.log.Timber

/**
 * Class used to decorate a plan. Plans contain routine UUIDs which have to be resolved before the plan is displayed on the UI
 */
class ELPlanDecorator {

    private val TAG = "ELPlanDecorator"

    fun decoratePlan(plan: ELPlan) {
        // Get routine UUIDs to resolve
        val routineUuids = plan.getRoutinesToResolve()
        if (routineUuids.isNotEmpty()) {
            // Decorate plans and routines
            plan.resolveRoutines(buildResolvedRoutines(routineUuids))
        }
    }

    private fun buildResolvedRoutines(routinesToResolve: Set<String>): Map<String, ELRoutine> {
        val resolvedRoutines = HashMap<String, ELRoutine>()
        routinesToResolve.forEach {
            // Check local routines cache first
            val cachedRoutine = resolveRoutine(it, Source.CACHE)
            if (cachedRoutine != null) {
                // Cache HIT - use cache and refresh
                Timber.tag(TAG).d("Cache HIT: item=%s", it)
                resolvedRoutines[it] = cachedRoutine
                Utils.runInBackground { resolveRoutine(it, Source.DEFAULT) }
            } else {
                // Cache MISS - fetch and use remote
                Timber.tag(TAG).d("Cache MISS: item=%s", it)
                val routine = resolveRoutine(it, Source.DEFAULT)
                if (routine != null) {
                    resolvedRoutines[it] = routine
                } else {
                    Timber.tag(TAG).d("Remote item is null: item=%s", it)
                }
            }
        }
        return resolvedRoutines
    }

    private fun resolveRoutine(uuid: String, source: Source): ELRoutine? {
        return try {
            val snapshot = Tasks.await(FirestorePathManager.routinesCollection.whereEqualTo(FieldPath.documentId(), uuid).get(source))
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().toObject(ELRoutine::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.tag(TAG).e(e)
            null
        }
    }
}