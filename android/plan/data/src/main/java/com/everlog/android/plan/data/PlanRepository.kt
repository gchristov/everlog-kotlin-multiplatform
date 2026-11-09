package com.everlog.android.plan.data

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.datasync.CollectionSource
import com.everlog.android.datasync.DataSync
import com.everlog.android.datasync.FirestoreCollection
import com.everlog.android.datasync.SyncPlan
import com.everlog.android.plan.data.model.ELPlan
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PlanRepository {
    /**
     * The plans the user made, as they change, in no particular order, once they've synced from the server at least
     * once. Each plan's own fields only: its days hold routine uuids, which this doesn't resolve. Empty without a
     * signed-in user.
     */
    fun plans(): Flow<Load<List<ELPlan>>>
}

@ContributesBinding(AppScope::class)
@Inject
class RealPlanRepository(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
    private val session: UserSession,
) : PlanRepository {

    // Plans live in one collection for everyone, so the user's are a query on who made them
    override fun plans(): Flow<Load<List<ELPlan>>> {
        val userId = session.user()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(
            CollectionSource(
                "plans",
                userId,
                SyncPlan.Whole,
                FirestoreCollection(
                    FirestorePaths.plans.whereEqualTo(FirestoreFields.CREATED_BY_USER_ID, userId),
                    dispatcher = dispatcher,
                ) { it.toObject(ELPlan::class.java) },
                // Plan details caches single plans (ELUserPlanStore), so the cache alone may hold only some of them
                completeOnly = true,
            ),
        )
    }
}
