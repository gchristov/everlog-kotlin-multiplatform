package com.everlog.android.auth.data

import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.common.persistence.PreferencesManager
import com.google.gson.Gson

// Keeps the user in memory once read, so the app holds one instance
class RealUserSession : PreferencesManager(), UserSession {

    private enum class PreferenceKeys {
        USER,
    }

    private var userAccountCache: ELUser? = null

    override fun user(): ELUser? {
        if (userAccountCache == null) {
            val json = getPreference(PreferenceKeys.USER.name, "")
            if (!json.isNullOrEmpty()) {
                userAccountCache = Gson().fromJson(json, ELUser::class.java)
            }
        }
        return userAccountCache
    }

    override fun updateUser(user: ELUser) {
        userAccountCache = user
        savePreference(Gson().toJson(user), PreferenceKeys.USER.name)
    }

    override fun clearUser() {
        userAccountCache = null
        removePreference(PreferenceKeys.USER.name)
    }
}
