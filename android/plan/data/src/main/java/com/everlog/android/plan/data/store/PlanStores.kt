package com.everlog.android.plan.data.store

import com.everlog.android.common.data.CommonData
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore
import com.everlog.android.plan.data.model.ELPlan

// The plan stores, one of each until destroy(). Test Lab runs keep plans in memory, so robots don't create data.
object PlanStores {

    private var plans: CollectionStore<ELPlan>? = null
    private var plan: DocumentStore<ELPlan>? = null

    @JvmStatic
    @Synchronized
    fun plansStore(): CollectionStore<ELPlan> =
        plans ?: (if (inMemory()) ELUserPlansInMemoryStore() else ELUserPlansStore()).also { plans = it }

    @JvmStatic
    @Synchronized
    fun planStore(): DocumentStore<ELPlan> =
        plan ?: (if (inMemory()) ELUserPlanInMemoryStore() else ELUserPlanStore()).also { plan = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        plans?.destroy()
        plans = null
        plan?.destroy()
        plan = null
    }

    private fun inMemory() = CommonData.dependencies.isTestLabRun()
}
