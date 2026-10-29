package com.everlog.ui.activities.base

import com.everlog.android.common.compose.CommonComposeActivity

/** A Compose screen for the signed-in user's data. One opened without a user closes instead of crashing. */
abstract class SignedInComposeActivity : CommonComposeActivity() {
    override fun canOpen(): Boolean = super.canOpen() && hasUser()
}
