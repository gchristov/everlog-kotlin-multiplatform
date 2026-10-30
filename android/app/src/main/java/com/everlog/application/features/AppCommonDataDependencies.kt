package com.everlog.application.features

import com.everlog.android.common.data.CommonDataDependencies
import com.everlog.utils.device.DeviceUtils

class AppCommonDataDependencies : CommonDataDependencies {

    override fun isTestLabRun(): Boolean = DeviceUtils.isFirebaseTestLabRun()
}
