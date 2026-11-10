package com.everlog.testutil

import android.app.Activity
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.everlog.android.home.screen.ComposeHomeActivity
import com.everlog.ui.activities.home.HomeActivity

/**
 * Both Homes, as the navigator opens the Compose one while Remote Config's home_v2 is on and today's otherwise:
 * end-to-end tests going through Home watch both, so they pass with the flag either way.
 */
class Homes(private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()) {
    private val monitors = listOf(HomeActivity::class.java, ComposeHomeActivity::class.java)
        .map { instrumentation.addMonitor(it.name, null, false) }

    val hits: Int get() = monitors.sumOf { it.hits }

    // The Home showing, if one is
    fun resumed(): Activity? {
        var resumed: Activity? = null
        instrumentation.runOnMainSync {
            val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            resumed = activities.firstOrNull { it is HomeActivity || it is ComposeHomeActivity }
        }
        return resumed
    }

    fun isCompose(): Boolean = resumed() is ComposeHomeActivity

    fun release() = monitors.forEach(instrumentation::removeMonitor)
}
