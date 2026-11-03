package com.everlog.android.pro

interface ProAnalytics {
    fun buyMonth()
    fun buyYear()
    fun purchased()
    fun cancelled()
    fun alreadyOwned()
    fun restore()
}

// The names analytics already has for the Pro screens
object ProScreenNames {
    const val PRO = "screen_pro"
}
