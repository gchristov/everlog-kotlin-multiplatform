package com.everlog.android.common.navigation

import android.content.Intent
import androidx.annotation.StringRes
import com.everlog.android.common.data.model.exercise.ELExerciseGroup

data object Home : Destination

data class Terms(val title: String) : Destination

data class PrivacyPolicy(val title: String) : Destination

data object PlayStoreListing : Destination

// [url] in the browser
data class WebLink(val url: String) : Destination

// The system share sheet with a link to the app
data object ShareApp : Destination

// Explains a missing permission, with a way to the app's system settings
data class AppSettingsPrompt(@param:StringRes val rationale: Int) : Destination

// Set suggestions, or the old muscle goal screen while Remote Config's set_suggestions_v2 is off
data object SetSuggestions : Destination

// The old exercise groups editor, for a finished workout's exercises
data class ExerciseGroupsEditor(val groups: List<ELExerciseGroup>) : ResultDestination<List<ELExerciseGroup>> {
    override val screenResult get() = Companion

    companion object : ScreenResult<List<ELExerciseGroup>> {
        override val requestCode = 7

        override fun read(data: Intent?): List<ELExerciseGroup>? = data.serializable(NavigationExtras.EXERCISE_GROUPS)
    }
}
