package com.everlog.config

import com.everlog.BuildConfig
import java.io.Serializable

data class HomeNotification (

        var title: String? = null,
        var description: String? = null,
        var imageUrl: String? = null,
        var actionId: String? = null,
        var actionUrl: String? = null,
        var minRequiredVersion: Int = 0,
        // Optional ISO-8601 instants (e.g. 2026-10-01T00:00:00Z) bounding when the banner may show.
        var startAt: String? = null,
        var endAt: String? = null

) : Serializable {

    enum class ActionType {
        NONE,
        MAINTENANCE,
        MUSCLE_GOALS,
        PLANS,
        SETTINGS,
        EXERCISES
    }

    fun canShow(): Boolean {
        return !title.isNullOrEmpty() && !description.isNullOrEmpty()
    }

    fun appUpdateRequired(versionCode: Int = BuildConfig.VERSION_CODE): Boolean {
        if (versionCode < minRequiredVersion) {
            return true
        }
        // actionUrl takes priority over actionId, so a usable url is always a supported action.
        if (hasSupportedUrl()) {
            return false
        }
        // An action id this app version doesn't know about was added in a newer release.
        return !actionId.isNullOrBlank() && getAction() == null
    }

    fun getAction(): ActionType? {
        return if (actionId == null) {
            null
        } else try {
            ActionType.valueOf(actionId!!)
        } catch (e: Exception) {
            null
        }
    }

    fun hasSupportedUrl(): Boolean {
        val url = actionUrl?.trim() ?: return false
        return url.startsWith("https://", true) || url.startsWith("http://", true)
    }
}
