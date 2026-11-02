package com.everlog.android.auth.testfixtures

import com.everlog.android.auth.data.UserSession
import com.everlog.android.auth.data.model.ELUser

class FakeUserSession(private var signedIn: ELUser? = ELUser(id = "user")) : UserSession {
    override fun user(): ELUser? = signedIn

    override fun updateUser(user: ELUser) {
        signedIn = user
    }

    override fun clearUser() {
        signedIn = null
    }
}
