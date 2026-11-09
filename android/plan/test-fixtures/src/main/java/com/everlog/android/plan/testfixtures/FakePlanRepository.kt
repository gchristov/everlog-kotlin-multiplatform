package com.everlog.android.plan.testfixtures

import com.everlog.android.common.kotlin.Load
import com.everlog.android.plan.data.PlanRepository
import com.everlog.android.plan.data.model.ELPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlanRepository : PlanRepository {
    // What plans() gives, changed by the test as it goes
    val plans = MutableStateFlow<Load<List<ELPlan>>>(Load.Loading)

    override fun plans(): Flow<Load<List<ELPlan>>> = plans
}
