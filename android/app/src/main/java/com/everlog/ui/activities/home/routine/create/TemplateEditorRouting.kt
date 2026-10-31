package com.everlog.ui.activities.home.routine.create

import android.content.Context
import android.content.Intent
import com.everlog.android.template.editor.TemplateEditorActivity
import com.everlog.managers.RemoteConfigManager

/**
 * The template editor for every entry point: the Compose one while Remote Config's template_editor_v2 is on, this
 * old one otherwise. A template to edit is passed by its uuid.
 */
fun templateEditorIntent(context: Context, properties: CreateRoutineActivity.Companion.Properties): Intent =
    if (RemoteConfigManager.manager.templateEditorV2()) {
        TemplateEditorActivity.launchIntent(context, properties.routine?.uuid, properties.showDetailsOnSuccess)
    } else {
        CreateRoutineActivity.launchIntent(context, properties)
    }
