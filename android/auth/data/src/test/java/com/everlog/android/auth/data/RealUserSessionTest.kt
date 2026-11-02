package com.everlog.android.auth.data

import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RealUserSessionTest {

    private val user = ELUser(id = "user", displayName = "Sam Lee")

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `has no user until one signs in`() {
        val session = RealUserSession()

        assertThat(session.user()).isNull()
        assertThrows(IllegalStateException::class.java) { session.requireUserId() }
    }

    @Test
    fun `keeps the signed-in user for the next launch`() {
        RealUserSession().updateUser(user)

        val next = RealUserSession()

        assertThat(next.user()).isEqualTo(user)
        assertThat(next.requireUserId()).isEqualTo("user")
    }

    @Test
    fun `forgets the user when cleared`() {
        val session = RealUserSession()
        session.updateUser(user)

        session.clearUser()

        assertThat(session.user()).isNull()
        assertThat(RealUserSession().user()).isNull()
    }
}
