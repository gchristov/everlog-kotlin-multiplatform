package com.everlog.ui.design

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.everlog.ui.activities.base.canOpen
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.ActivityUtils

abstract class CommonComposeActivity : ComponentActivity() {
    @Composable
    protected abstract fun Content()

    /** Intent extras the screen can't work without. A screen opened without them closes instead of crashing. */
    protected open val requiredExtras: List<String> = emptyList()

    /** Whether the screen needs a logged in user. One opened without a user closes instead of crashing. */
    protected open val requiresUser: Boolean = false

    /** False when the screen closed straight away for missing [requiredExtras] or user: skip its own set-up. */
    protected var isOpened = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light system bar icons, as the app is always dark
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (!canOpen(requiredExtras, requiresUser)) {
            finish()
            return
        }
        isOpened = true
        // Portrait on phones, any orientation on tablets, like the rest of the app
        ActivityUtils.setOrientation(this)
        setContent {
            Theme {
                Content()
            }
        }
    }
}
