package com.everlog.gradleplugins.modules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModuleRulesTest {

    @Test
    fun `reads module paths`() {
        assertThat(Module.of(":android:app")).isEqualTo(Module.App)
        assertThat(Module.of(":android:common:compose-test")).isEqualTo(Module.Common("compose-test"))
        assertThat(Module.of(":android:exercise:test-fixtures"))
            .isEqualTo(Module.Feature("exercise", FeatureModuleType.TEST_FIXTURES))
        assertThat(Module.of(":android:exercise:other")).isNull()
        assertThat(Module.of(":gradle-plugins:conventions")).isNull()
    }

    @Test
    fun `nothing depends on the app`() {
        assertThat(broken(":android:common:compose", ":android:app")).isNotNull()
        assertThat(broken(":android:exercise:feature", ":android:app", forTests = true)).isNotNull()
    }

    @Test
    fun `the app depends on anything but test support outside its tests`() {
        assertThat(broken(":android:app", ":android:exercise:feature")).isNull()
        assertThat(broken(":android:app", ":android:common:mvp")).isNull()
        assertThat(broken(":android:app", ":android:common:test")).isNotNull()
        assertThat(broken(":android:app", ":android:common:test", forTests = true)).isNull()
    }

    @Test
    fun `common modules depend only on common modules`() {
        assertThat(broken(":android:common:compose", ":android:common:design")).isNull()
        assertThat(broken(":android:common:kotlin", ":android:exercise:data")).isNotNull()
    }

    @Test
    fun `a feature depends only on its own modules`() {
        assertThat(broken(":android:exercise:feature", ":android:exercise:data")).isNull()
        assertThat(broken(":android:exercise:feature", ":android:workout:feature")).isNotNull()
        assertThat(broken(":android:exercise:feature", ":android:workout:data")).isNotNull()
        assertThat(broken(":android:exercise:feature", ":android:exercise:test-fixtures", forTests = true)).isNull()
    }

    @Test
    fun `data depends on other data but never on UI`() {
        assertThat(broken(":android:workout:data", ":android:exercise:data")).isNull()
        assertThat(broken(":android:workout:data", ":android:exercise:test-fixtures", forTests = true)).isNull()
        assertThat(broken(":android:workout:data", ":android:workout:feature")).isNotNull()
        assertThat(broken(":android:workout:data", ":android:common:compose")).isNotNull()
        assertThat(broken(":android:workout:data", ":android:common:mvp")).isNotNull()
        assertThat(broken(":android:workout:data", ":android:common:design")).isNotNull()
    }

    @Test
    fun `test fixtures depend on data but not on another feature's UI`() {
        assertThat(broken(":android:workout:test-fixtures", ":android:workout:feature")).isNull()
        assertThat(broken(":android:workout:test-fixtures", ":android:exercise:data")).isNull()
        assertThat(broken(":android:workout:test-fixtures", ":android:common:test")).isNull()
        assertThat(broken(":android:workout:test-fixtures", ":android:exercise:feature")).isNotNull()
    }

    @Test
    fun `test support is only for tests`() {
        assertThat(broken(":android:exercise:feature", ":android:exercise:test-fixtures")).isNotNull()
        assertThat(broken(":android:exercise:data", ":android:common:test")).isNotNull()
        assertThat(broken(":android:common:compose-test", ":android:common:test")).isNull()
        assertThat(broken(":android:exercise:data", ":android:common:data-test")).isNotNull()
        assertThat(broken(":android:exercise:data", ":android:common:data-test", forTests = true)).isNull()
    }

    private fun broken(from: String, to: String, forTests: Boolean = false) =
        moduleRuleBroken(Module.of(from)!!, Module.of(to)!!, forTests)
}
