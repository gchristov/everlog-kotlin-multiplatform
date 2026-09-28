package com.everlog.managers.apprate

import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RatePromptControllerTest {

    @Test
    fun `launches on a showing, uncovered screen`() {
        assertThat(RatePromptController.canLaunch(Lifecycle.State.RESUMED, isFinishing = false, hasWindowFocus = true)).isTrue()
    }

    @Test
    fun `doesn't launch once the user has left the screen or the app`() {
        assertThat(RatePromptController.canLaunch(Lifecycle.State.STARTED, isFinishing = false, hasWindowFocus = true)).isFalse()
        assertThat(RatePromptController.canLaunch(Lifecycle.State.CREATED, isFinishing = false, hasWindowFocus = false)).isFalse()
        assertThat(RatePromptController.canLaunch(Lifecycle.State.DESTROYED, isFinishing = true, hasWindowFocus = false)).isFalse()
    }

    @Test
    fun `doesn't launch while the screen is closing`() {
        assertThat(RatePromptController.canLaunch(Lifecycle.State.RESUMED, isFinishing = true, hasWindowFocus = true)).isFalse()
    }

    @Test
    fun `doesn't launch over a dialog or share sheet`() {
        assertThat(RatePromptController.canLaunch(Lifecycle.State.RESUMED, isFinishing = false, hasWindowFocus = false)).isFalse()
    }
}
