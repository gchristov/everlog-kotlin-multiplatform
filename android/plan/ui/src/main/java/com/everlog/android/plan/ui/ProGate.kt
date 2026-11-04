package com.everlog.android.plan.ui

import android.content.Context
import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.ProBuy

// Runs [block] for a Pro user, and offers Pro to anyone else
fun runProFeature(context: Context, session: UserSession, navigator: Navigator, block: () -> Unit) {
    if (session.user()?.isPro() == true) block() else navigator.open(context, ProBuy)
}
