package com.everlog.gradleplugins.modules

import com.everlog.gradleplugins.modules.FeatureModuleType.DATA
import com.everlog.gradleplugins.modules.FeatureModuleType.FEATURE
import com.everlog.gradleplugins.modules.FeatureModuleType.TEST_FIXTURES

/**
 * Why [from] can't depend on [to], or null when it can. [forTests] is true for test configurations
 * (testImplementation, androidTestImplementation and the like). The rules are described in android/README.md.
 */
internal fun moduleRuleBroken(from: Module, to: Module, forTests: Boolean): String? = when {
    to == Module.App -> "nothing depends on the app"
    to.isTestSupport && !forTests && !from.isTestSupport -> "test support is only for tests"
    else -> when (from) {
        Module.App -> null
        is Module.Common -> commonRuleBroken(to)
        is Module.Feature -> when (from.type) {
            DATA -> dataRuleBroken(to)
            FEATURE -> featureRuleBroken(from, to)
            TEST_FIXTURES -> testFixturesRuleBroken(from, to)
        }
    }
}

private fun commonRuleBroken(to: Module) =
    if (to is Module.Feature) "common modules never depend on feature modules" else null

private fun dataRuleBroken(to: Module) = when {
    to is Module.Feature && to.type == FEATURE -> "data never depends on UI"
    to is Module.Common && to.isUi -> "data never depends on UI"
    else -> null
}

private fun featureRuleBroken(from: Module.Feature, to: Module) =
    if (to is Module.Feature && to.feature != from.feature) "a feature never depends on another feature" else null

private fun testFixturesRuleBroken(from: Module.Feature, to: Module) =
    if (to is Module.Feature && to.feature != from.feature && to.type == FEATURE) {
        "a feature never depends on another feature"
    } else {
        null
    }
