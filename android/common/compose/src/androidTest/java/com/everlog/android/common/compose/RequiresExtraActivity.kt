package com.everlog.android.common.compose

import androidx.compose.runtime.Composable
import com.everlog.android.common.compose.elements.AppText

class RequiresExtraActivity : CommonComposeActivity() {
    override val requiredExtras = listOf(EXTRA)

    @Composable
    override fun Content() = AppText(text = "Opened")

    companion object {
        const val EXTRA = "required"
    }
}
