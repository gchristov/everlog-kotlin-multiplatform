package com.everlog.managers.analytics

import android.app.Activity
import android.os.Bundle
import com.everlog.application.ELApplication.Companion.getInstance
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalytic : BaseAnalytic() {

    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    init {
        setupFirebase()
    }

    override fun toggleAnalytics(enabled: Boolean) {
        super.toggleAnalytics(enabled)
        mFirebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }

    override fun logScreenName(eventName: String,
                               activity: Activity?,
                               screenName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        if (activity != null) {
            mFirebaseAnalytics?.setCurrentScreen(activity, screenName, screenName)
        }
    }

    override fun logUserRegister(eventName: String,
                                 userId: String?,
                                 email: String?,
                                 displayName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        val bundle = Bundle()
        bundle.putString(AnalyticsConstants.PROPERTY_USER_ID, userId)
        bundle.putString(AnalyticsConstants.PROPERTY_USER_EMAIL, email)
        bundle.putString(AnalyticsConstants.PROPERTY_USER_DISPLAY_NAME, displayName)
        mFirebaseAnalytics?.logEvent(eventName, bundle)
    }

    override fun logUserIdentify(eventName: String,
                                 userId: String?,
                                 email: String?,
                                 displayName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        val bundle = Bundle()
        bundle.putString(AnalyticsConstants.PROPERTY_USER_ID, userId)
        bundle.putString(AnalyticsConstants.PROPERTY_USER_EMAIL, email)
        bundle.putString(AnalyticsConstants.PROPERTY_USER_DISPLAY_NAME, displayName)
        mFirebaseAnalytics?.setUserId(userId)
        mFirebaseAnalytics?.setUserProperty(AnalyticsConstants.PROPERTY_USER_ID, userId)
        mFirebaseAnalytics?.setUserProperty(AnalyticsConstants.PROPERTY_USER_EMAIL, email)
        mFirebaseAnalytics?.setUserProperty(AnalyticsConstants.PROPERTY_USER_DISPLAY_NAME, displayName)
        mFirebaseAnalytics?.logEvent(eventName, bundle)
    }

    override fun logUserLogout(eventName: String) {
        if (!mAnalyticsEnabled) {
            return
        }
        mFirebaseAnalytics?.logEvent(eventName, Bundle())
    }

    override fun logEvent(name: String) {
        if (!mAnalyticsEnabled) {
            return
        }
        mFirebaseAnalytics?.logEvent(name, Bundle())
    }

    override fun logEvent(name: String, data: Map<String, Any?>) {
        if (!mAnalyticsEnabled) {
            return
        }
        mFirebaseAnalytics?.logEvent(name, buildBundleFromMap(data))
    }

    // Setup

    private fun setupFirebase() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getInstance())
    }
}