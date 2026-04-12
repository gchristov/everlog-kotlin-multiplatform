package com.everlog.managers.auth

import android.text.TextUtils
import com.everlog.data.model.ELUser
import com.everlog.managers.preferences.PreferencesManager
import com.google.gson.Gson

object LocalUserManager : PreferencesManager() {

    private enum class PreferenceKeys {
        USER,
    }

    private var userAccountCache: ELUser? = null

    @JvmStatic
    fun getUser(): ELUser? {
        if (userAccountCache == null) {
            val json = getPreference(PreferenceKeys.USER.name, "")
            if (!TextUtils.isEmpty(json)) {
                userAccountCache = Gson().fromJson(json, ELUser::class.java)
            }
        }
        return userAccountCache
    }

    fun updateUser(user: ELUser) {
        userAccountCache = user
        savePreference(Gson().toJson(user), PreferenceKeys.USER.name)
    }

    @JvmStatic
    fun hasUser(): Boolean {
        return getUser() != null
    }

    fun clearUser() {
        userAccountCache = null
        removePreference(PreferenceKeys.USER.name)
    }
}