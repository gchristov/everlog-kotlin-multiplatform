package com.everlog.android.common.data.model.set

import com.everlog.android.common.kotlin.format.setTypeDescriptionRes
import com.everlog.android.common.kotlin.format.setTypeTitleRes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELSetTypeTextTest {

    @Test
    fun `every set type has a title and a description`() {
        ELSetType.values().forEach { type ->
            assertThat(setTypeTitleRes(type.name)).isNotNull()
            assertThat(setTypeDescriptionRes(type.name)).isNotNull()
        }
    }
}
