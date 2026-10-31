package com.everlog.android.template.perform

import com.everlog.android.template.details.MvpViewRoutineDetails

interface MvpViewPerformRoutine : MvpViewRoutineDetails {

    fun performingFromPlan(): Boolean
}