package com.everlog.config

import com.everlog.BuildConfig
import java.io.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

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

    /** What should happen when the banner is tapped. */
    sealed class TapAction {
        object OpenPlayStore : TapAction()
        data class OpenUrl(val url: String) : TapAction()
        data class OpenScreen(val type: ActionType) : TapAction()
        /** Nothing sensible to do, e.g. a pure announcement or an unsupported action. */
        object None : TapAction()
    }

    fun canShow(): Boolean {
        return !title.isNullOrEmpty() && !description.isNullOrEmpty()
    }

    fun isWithinSchedule(now: Instant): Boolean {
        if (!startAt.isNullOrBlank()) {
            val start = parseInstant(startAt) ?: return false
            if (now.isBefore(start)) return false
        }
        if (!endAt.isNullOrBlank()) {
            val end = parseInstant(endAt) ?: return false
            if (!now.isBefore(end)) return false
        }
        return true
    }

    /** Everything the banner itself decides, i.e. excluding whether the user already dismissed it. */
    fun isEligible(now: Instant = Instant.now()): Boolean {
        return canShow() && isWithinSchedule(now)
    }

    fun appUpdateRequired(versionCode: Int = BuildConfig.VERSION_CODE): Boolean {
        if (versionCode < minRequiredVersion) {
            return true
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

    fun resolveTapAction(versionCode: Int = BuildConfig.VERSION_CODE): TapAction {
        if (appUpdateRequired(versionCode)) {
            return TapAction.OpenPlayStore
        }
        val url = actionUrl?.trim()
        if (isSupportedUrl(url)) {
            return TapAction.OpenUrl(url!!)
        }
        return when (val action = getAction()) {
            null, ActionType.NONE, ActionType.MAINTENANCE -> TapAction.None
            else -> TapAction.OpenScreen(action)
        }
    }

    private fun isSupportedUrl(url: String?): Boolean {
        return url != null && (url.startsWith("https://", true) || url.startsWith("http://", true))
    }

    private fun parseInstant(value: String?): Instant? {
        return try {
            Instant.parse(value!!.trim())
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
