package com.everlog.android.auth.data

import com.everlog.android.auth.data.model.ELUser

// The signed-in user, kept on the device between launches
interface UserSession {
    // Null when signed out
    fun user(): ELUser?

    fun updateUser(user: ELUser)

    fun clearUser()
}

// For what only happens while signed in
fun UserSession.requireUser(): ELUser = user() ?: throw IllegalStateException("User missing or invalid")

fun UserSession.requireUserId(): String = requireUser().id ?: throw IllegalStateException("User missing or invalid")
