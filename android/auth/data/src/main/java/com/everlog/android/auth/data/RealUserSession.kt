package com.everlog.android.auth.data

import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.common.persistence.PreferencesManager
import com.google.gson.Gson
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

// Keeps the user in memory once read, so the app holds one instance
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserSession>())
@Inject
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
