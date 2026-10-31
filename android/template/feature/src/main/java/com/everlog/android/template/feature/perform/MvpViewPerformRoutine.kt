package com.everlog.android.template.feature.perform

import com.everlog.android.template.feature.details.MvpViewRoutineDetails

interface MvpViewPerformRoutine : MvpViewRoutineDetails {

    fun performingFromPlan(): Boolean
}