package com.everlog.android.settings.integration

import androidx.annotation.DrawableRes
import com.everlog.android.settings.R
import com.everlog.android.settings.data.model.ELIntegration

@DrawableRes
fun ELIntegration.Type.getIcon(): Int? {
    return when (this) {
        ELIntegration.Type.GOOGLE_FIT -> R.drawable.ic_integration_google_fit
    }
}
