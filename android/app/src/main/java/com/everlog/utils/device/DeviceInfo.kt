package com.everlog.utils.device

import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.android.common.kotlin.AppContext
import java.util.*

object DeviceInfo {

    val appInfo: String
        get() = String.format(Locale.getDefault(),
                "%s %s (%d)",
                AppContext.getString(R.string.everlog_app_name), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
}
