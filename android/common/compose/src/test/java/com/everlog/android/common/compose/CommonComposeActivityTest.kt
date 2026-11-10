package com.everlog.android.common.compose

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.android.common.composetest.FeatureTestDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [FeatureTestDevice.Sdk])
class CommonComposeActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        // A test activity, in no manifest
        shadowOf(context.packageManager).addActivityIfNotPresent(ComponentName(context, RequiresExtraActivity::class.java))
    }

    @Test
    fun closesWithoutItsRequiredExtras() {
        ActivityScenario.launch<RequiresExtraActivity>(Intent(context, RequiresExtraActivity::class.java)).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun opensWithItsRequiredExtras() {
        val intent = Intent(context, RequiresExtraActivity::class.java).putExtra(RequiresExtraActivity.EXTRA, true)
        ActivityScenario.launch<RequiresExtraActivity>(intent).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }
}
