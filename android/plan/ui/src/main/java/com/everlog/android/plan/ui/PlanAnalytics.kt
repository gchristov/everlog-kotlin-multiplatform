package com.everlog.android.plan.ui

interface PlanAnalytics {
    fun created()
    fun modified()
    fun deleted()
    fun started()
    fun stopped()
    fun weekAdded()
    fun weekDeleted()
    fun daySetToTemplate()
    fun daySetToRest()
}
