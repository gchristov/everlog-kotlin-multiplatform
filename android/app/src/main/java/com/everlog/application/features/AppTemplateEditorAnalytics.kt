package com.everlog.application.features

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.session.ValueField
import com.everlog.android.template.TemplateScreenNames
import com.everlog.android.template.editor.TemplateEditorAnalytics
import com.everlog.managers.analytics.AnalyticsManager

class AppTemplateEditorAnalytics : TemplateEditorAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(TemplateScreenNames.EDITOR)

    override fun templateSaved(isNew: Boolean) = if (isNew) analytics.routineCreated() else analytics.routineModified()

    override fun exercisesAdded() = analytics.routineExerciseGroupAdded()

    override fun setAdded() = analytics.setAdded()

    override fun setDeleted() = analytics.setDeleted()

    override fun restChanged(seconds: Int) = analytics.setRestTimeModified(seconds)

    override fun valueChanged(field: ValueField, value: Int?) = when (field) {
        ValueField.Time -> analytics.setRequiredTimeModified(value)
        else -> analytics.setRequiredRepsModified(value)
    }

    override fun timeForAllSets(seconds: Int) = analytics.setRequiredTimeModified(seconds)

    override fun setTypeChanged(type: ELSetType) = analytics.setTypeSelected(type)

    override fun leavePromptShown() = analytics.routineLeavePromptShown()

    override fun leavePromptSaved() = analytics.routineLeavePromptSaved()

    override fun leavePromptDiscarded() = analytics.routineLeavePromptDiscarded()

    override fun leavePromptCancelled() = analytics.routineLeavePromptCancelled()
}
