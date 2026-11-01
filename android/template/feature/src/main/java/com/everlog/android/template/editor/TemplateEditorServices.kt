package com.everlog.android.template.editor

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.session.ValueField

interface TemplateEditorAnalytics {
    // Logged as the screen shows, before its other events, so they're credited to it
    fun screenViewed()

    fun templateSaved(isNew: Boolean)

    fun exercisesAdded()

    fun setAdded()

    fun setDeleted()

    fun restChanged(seconds: Int)

    // A set's reps, or its time in seconds, once typed or stepped
    fun valueChanged(field: ValueField, value: Int?)

    // One time given to every set of an exercise
    fun timeForAllSets(seconds: Int)

    // Exercises made a superset or circuit, or split (SINGLE)
    fun setTypeChanged(type: ELSetType)

    // Save your changes?, asked on leaving with unsaved changes, and what was picked
    fun leavePromptShown()

    fun leavePromptSaved()

    fun leavePromptDiscarded()

    fun leavePromptCancelled()
}
