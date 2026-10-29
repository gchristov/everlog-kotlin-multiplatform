package com.everlog.managers.auth

import com.everlog.data.model.ELUser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SavedUserTest {

    private val saved = ELUser().apply { id = "uid" }

    @Test
    fun `the saved user of the signed-in account is carried on with`() {
        assertThat(savedUserFor("uid", saved)).isSameInstanceAs(saved)
    }

    @Test
    fun `another account's saved user, or none, waits for the document`() {
        assertThat(savedUserFor("other", saved)).isNull()
        assertThat(savedUserFor("uid", null)).isNull()
    }
}
