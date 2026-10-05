package com.everlog.ui.design

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.ActivityUtils

abstract class CommonComposeActivity : ComponentActivity() {
    @Composable
    protected abstract fun Content()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light system bar icons, as the app is always dark
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Portrait on phones, any orientation on tablets, like the rest of the app
        ActivityUtils.setOrientation(this)
        setContent {
            Theme {
                Content()
            }
        }
    }
}
