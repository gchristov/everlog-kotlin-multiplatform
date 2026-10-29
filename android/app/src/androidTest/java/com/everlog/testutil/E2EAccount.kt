package com.everlog.testutil

import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.BuildConfig
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.auth.LocalUserManager
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Signs in with the E2E test account (BuildConfig's E2E_TEST_USER_EMAIL and PASSWORD, as LoginActivityTest uses),
 * unless someone is signed in already, so screens that read or write Firestore work in end-to-end tests. Data a
 * test saves to the account should be deleted by the test.
 */
object E2EAccount {
    fun signIn() {
        if (LocalUserManager.getUser() != null) return
        val signedIn = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AuthManager.login(BuildConfig.E2E_TEST_USER_EMAIL, BuildConfig.E2E_TEST_USER_PASSWORD, object : AuthManager.OnAuthActionListener() {
                override fun onSuccess(user: ELUser, newUser: Boolean) = signedIn.countDown()

                override fun onError(throwable: Throwable) {
                    error = throwable
                    signedIn.countDown()
                }
            })
        }
        assertThat(signedIn.await(30, TimeUnit.SECONDS)).isTrue()
        error?.let { throw it }
    }
}
