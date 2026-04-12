package com.everlog.managers.analytics

import android.app.Activity
import io.customerly.Customerly

class CustomerlyAnalytic : BaseAnalytic() {

    override fun logScreenName(eventName: String, activity: Activity?, screenName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        if (screenName != null) {
            Customerly.trackEvent(screenName)
        }
    }

    override fun logUserRegister(eventName: String, userId: String?, email: String?, displayName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        Customerly.trackEvent(eventName)
    }

    override fun logUserIdentify(eventName: String, userId: String?, email: String?, displayName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        Customerly.trackEvent(eventName)
    }

    override fun logUserLogout(eventName: String) {
        if (!mAnalyticsEnabled) {
            return
        }
        Customerly.trackEvent(eventName)
    }

    override fun logEvent(name: String) {
        if (!mAnalyticsEnabled) {
            return
        }
        Customerly.trackEvent(name)
    }

    override fun logEvent(name: String, data: Map<String, Any?>) {
        if (!mAnalyticsEnabled) {
            return
        }
        Customerly.trackEvent(name)
    }
}