package com.everlog.config

import android.text.TextUtils
import com.everlog.BuildConfig
import java.io.Serializable

data class HomeNotification (

        var title: String? = null,
        var description: String? = null,
        var imageUrl: String? = null,
        var actionId: String? = null,
        var minRequiredVersion: Int = 0

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
        return !TextUtils.isEmpty(title) && !TextUtils.isEmpty(description)
    }

    fun appUpdateRequired(): Boolean {
        val action = getAction() ?: return true
        return if (action == ActionType.MAINTENANCE) {
            false
        } else BuildConfig.VERSION_CODE < minRequiredVersion
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
}