package com.everlog.android.common.composetest

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.AppContext
import com.jakewharton.threetenabp.AndroidThreeTen
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A feature test: one screen driven as a user would, with its real view-model over fakes, on the JVM with
 * Robolectric rather than an emulator. Name the test class `<Screen>FeatureTest` and put it in the module's
 * `src/test`. Every feature test runs on the same phone and Android version, with what the app sets up at startup.
 *
 * Robolectric runs the test on its main thread, so JUnit's Timeout rule can't be used, and its main looper only runs
 * when told to: wait with [waitUntil] rather than the compose rule's, which doesn't run it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [FeatureTestDevice.Sdk], qualifiers = FeatureTestDevice.Qualifiers)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class FeatureTest {

    @get:Rule
    val compose = createComposeRule()

    protected val context: Context = ApplicationProvider.getApplicationContext()

    init {
        // Set by the app at startup, which a feature test doesn't run
        AppContext = context
        AndroidThreeTen.init(context)
    }

    // The screen in the app's theme
    protected fun setContent(content: @Composable () -> Unit) = compose.setContent { Theme { content() } }

    // Runs the main looper on every check, for what the screen hands to it: a view-model's coroutines on the main
    // dispatcher, a snackbar's result, a local broadcast
    protected fun waitUntil(timeoutMillis: Long = DefaultWaitMillis, condition: () -> Boolean) =
        compose.waitUntil(timeoutMillis) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }

    private companion object {
        const val DefaultWaitMillis = 5_000L
    }
}

// A Pixel 8-sized phone on the Android version the app targets
object FeatureTestDevice {
    const val Sdk = 35
    const val Qualifiers = "w411dp-h914dp-xxhdpi"
}
