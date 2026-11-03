package com.everlog.android.template

import com.everlog.android.common.navigation.Navigator
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.template.data.session.LoadTemplateUseCase
import com.everlog.android.template.data.session.OngoingPlanRepository
import com.everlog.android.template.data.session.SaveTemplateUseCase
import com.everlog.android.template.editor.TemplateEditorAnalytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the template screens get from the app's graph, through appGraph<TemplateGraph>()
@ContributesTo(AppScope::class)
interface TemplateGraph {
    val userSettings: UserSettings

    val loadTemplate: LoadTemplateUseCase

    val saveTemplate: SaveTemplateUseCase

    // The user's own exercises as they change, so the editor shows a rename straight away
    val exerciseRepository: ExerciseRepository

    val ongoingPlanRepository: OngoingPlanRepository

    val templateAnalytics: TemplateAnalytics

    val templateEditorAnalytics: TemplateEditorAnalytics

    val navigator: Navigator
}
