package com.everlog.managers.analytics

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

    override fun logScreenName(eventName: String, screenName: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        // Replaces the deprecated setCurrentScreen(). Firebase ignores screen views while the app
        // is in the background, so events from there (e.g. the workout notification) have none.
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        mFirebaseAnalytics?.logEvent(eventName, bundle)
    }

    override fun logUserRegister(eventName: String, userId: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        val bundle = Bundle()
        bundle.putString(AnalyticsConstants.PROPERTY_USER_ID, userId)
        mFirebaseAnalytics?.logEvent(eventName, bundle)
    }

    override fun logUserIdentify(eventName: String, userId: String?) {
        if (!mAnalyticsEnabled) {
            return
        }
        val bundle = Bundle()
        bundle.putString(AnalyticsConstants.PROPERTY_USER_ID, userId)
        mFirebaseAnalytics?.setUserId(userId)
        mFirebaseAnalytics?.setUserProperty(AnalyticsConstants.PROPERTY_USER_ID, userId)
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