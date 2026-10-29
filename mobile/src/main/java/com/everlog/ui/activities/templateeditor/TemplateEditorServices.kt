package com.everlog.ui.activities.templateeditor

import com.everlog.data.model.set.ELSetType
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager

internal interface TemplateEditorAnalytics {
    // Logged as the screen shows, before its other events, so they're credited to it
    fun screenViewed()

    fun templateSaved(isNew: Boolean)

    fun exercisesAdded()

    fun setAdded()

    fun setDeleted()

    fun restChanged(seconds: Int)

    // One time given to every set of an exercise
    fun timeForAllSets(seconds: Int)

    // Exercises made a superset or circuit, or split (SINGLE)
    fun setTypeChanged(type: ELSetType)
}

internal class RealTemplateEditorAnalytics : TemplateEditorAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(AnalyticsConstants.SCREEN_ROUTINE_CREATE_V2)

    override fun templateSaved(isNew: Boolean) = if (isNew) analytics.routineCreated() else analytics.routineModified()

    override fun exercisesAdded() = analytics.routineExerciseGroupAdded()

    override fun setAdded() = analytics.setAdded()

    override fun setDeleted() = analytics.setDeleted()

    override fun restChanged(seconds: Int) = analytics.setRestTimeModified(seconds)

    override fun timeForAllSets(seconds: Int) = analytics.setRequiredTimeModified(seconds)

    override fun setTypeChanged(type: ELSetType) = analytics.setTypeSelected(type)
}
