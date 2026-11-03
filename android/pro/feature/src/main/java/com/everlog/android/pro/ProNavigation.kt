package com.everlog.android.pro

import android.content.Context

// The other screens the Pro screen opens, through the app's Navigator
interface ProNavigation {
    fun openTerms(context: Context, title: String)

    fun openPrivacyPolicy(context: Context, title: String)

    fun openCongratulate(context: Context)
}
