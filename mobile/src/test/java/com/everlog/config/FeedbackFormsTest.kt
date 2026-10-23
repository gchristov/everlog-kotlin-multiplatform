package com.everlog.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedbackFormsTest {

    // RemoteConfig parses the `feedback_forms` string with RemoteConfig.parse, so test that exact path.
    private fun parse(json: String) = RemoteConfig.parse(json, FeedbackForms::class.java)

    @Test
    fun `parses a url for every form`() {
        val forms = parse("""
            {
              "general": "https://forms.gle/general",
              "statistics": "https://forms.gle/statistics",
              "setTypes": "https://forms.gle/set-types",
              "setSuggestions": "https://forms.gle/set-suggestions",
              "workout": "https://forms.gle/workout",
              "exercisePicker": "https://forms.gle/exercise-picker"
            }
        """.trimIndent())!!
        assertThat(forms.url(FeedbackSource.SETTINGS, "2.21.0")).isEqualTo("https://forms.gle/general")
        assertThat(forms.url(FeedbackSource.ACTIVITY_STATISTICS, "2.21.0")).isEqualTo("https://forms.gle/statistics")
        assertThat(forms.url(FeedbackSource.EXERCISE_STATISTICS, "2.21.0")).isEqualTo("https://forms.gle/statistics")
        assertThat(forms.url(FeedbackSource.SET_TYPES, "2.21.0")).isEqualTo("https://forms.gle/set-types")
        assertThat(forms.url(FeedbackSource.MUSCLE_GOAL, "2.21.0")).isEqualTo("https://forms.gle/set-suggestions")
        assertThat(forms.url(FeedbackSource.SET_SUGGESTIONS, "2.21.0")).isEqualTo("https://forms.gle/set-suggestions")
        assertThat(forms.url(FeedbackSource.WORKOUT_MENU, "2.21.0")).isEqualTo("https://forms.gle/workout")
        assertThat(forms.url(FeedbackSource.EXERCISE_PICKER_MENU, "2.21.0")).isEqualTo("https://forms.gle/exercise-picker")
        assertThat(forms.url(FeedbackSource.EXERCISE_PICKER_NO_RESULTS, "2.21.0")).isEqualTo("https://forms.gle/exercise-picker")
    }

    @Test
    fun `empty default has no forms`() {
        val forms = parse("{}")!!
        FeedbackSource.values().forEach { assertThat(forms.url(it, "2.21.0")).isNull() }
    }

    @Test
    fun `a missing or blank url has no form`() {
        val forms = FeedbackForms(general = "  ", statistics = "")
        assertThat(forms.url(FeedbackSource.SETTINGS, "2.21.0")).isNull()
        assertThat(forms.url(FeedbackSource.ACTIVITY_STATISTICS, "2.21.0")).isNull()
        assertThat(forms.url(FeedbackSource.WORKOUT_MENU, "2.21.0")).isNull()
    }

    @Test
    fun `fills in the app version and source`() {
        val forms = FeedbackForms(exercisePicker = " https://docs.google.com/forms/d/e/abc/viewform?entry.1={appVersion}&entry.2={source} ")
        assertThat(forms.url(FeedbackSource.EXERCISE_PICKER_NO_RESULTS, "2.21.0 (dev)"))
                .isEqualTo("https://docs.google.com/forms/d/e/abc/viewform?entry.1=2.21.0+%28dev%29&entry.2=exercise_picker_no_results")
    }

    @Test
    fun `unknown keys are ignored so newer configs stay compatible`() {
        val forms = parse("""{"general":"https://forms.gle/general","onboarding":"https://forms.gle/onboarding"}""")!!
        assertThat(forms.url(FeedbackSource.SETTINGS, "2.21.0")).isEqualTo("https://forms.gle/general")
    }

    @Test
    fun `a malformed value is dropped`() {
        assertThat(parse("""["https://forms.gle/general"]""")).isNull()
    }
}
