package com.everlog.ui.navigator

import android.app.Activity
import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager

/**
 * Opens [source]'s feedback form, or null when Remote Config has none, so a Compose screen leaves its entry point
 * out.
 */
fun Activity.feedbackFormLauncher(source: FeedbackSource): (() -> Unit)? =
    if (RemoteConfigManager.manager.feedbackFormUrl(source) != null) {
        { ELNavigator(this).openFeedbackForm(source) }
    } else {
        null
    }
