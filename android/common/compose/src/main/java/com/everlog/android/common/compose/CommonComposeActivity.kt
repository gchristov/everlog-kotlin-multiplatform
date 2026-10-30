package com.everlog.android.common.compose

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.hasRequiredExtras
import com.everlog.android.common.design.R as DesignR

abstract class CommonComposeActivity : ComponentActivity() {
    @Composable
    protected abstract fun Content()

    /** Intent extras the screen can't work without. A screen opened without them closes instead of crashing. */
    protected open val requiredExtras: List<String> = emptyList()

    /** False when the screen closed straight away because [canOpen] said no: skip its own set-up. */
    protected var isOpened = false
        private set

    /** Whether the screen has what it needs to open. Screens with further requirements add to it. */
    protected open fun canOpen(): Boolean = hasRequiredExtras(requiredExtras)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light system bar icons, as the app is always dark
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (!canOpen()) {
            finish()
            return
        }
        isOpened = true
        // Portrait on phones, any orientation on tablets, like the rest of the app
        if (!resources.getBoolean(DesignR.bool.isTablet)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent {
            Theme {
                Content()
            }
        }
    }
}
