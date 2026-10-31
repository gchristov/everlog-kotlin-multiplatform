package com.everlog.android.template.editor

import com.everlog.android.common.data.model.set.ELSetType

interface TemplateEditorAnalytics {
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
