package com.everlog.ui.activities.home.routine.perform

import com.everlog.ui.activities.home.routine.details.MvpViewRoutineDetails

interface MvpViewPerformRoutine : MvpViewRoutineDetails {

    fun performingFromPlan(): Boolean
}