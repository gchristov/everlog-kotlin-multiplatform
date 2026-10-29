package com.everlog.android.common.compose

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CommonComposeActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun closesWithoutItsRequiredExtras() {
        ActivityScenario.launch<RequiresExtraActivity>(Intent(context, RequiresExtraActivity::class.java)).use {
            assertThat(it.awaitState(Lifecycle.State.DESTROYED)).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun opensWithItsRequiredExtras() {
        val intent = Intent(context, RequiresExtraActivity::class.java).putExtra(RequiresExtraActivity.EXTRA, true)
        ActivityScenario.launch<RequiresExtraActivity>(intent).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    private fun ActivityScenario<*>.awaitState(expected: Lifecycle.State): Lifecycle.State {
        val until = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (state != expected && SystemClock.uptimeMillis() < until) SystemClock.sleep(50)
        return state
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
