package com.everlog.gradleplugins.modules

import com.everlog.gradleplugins.modules.FeatureModuleType.DATA
import com.everlog.gradleplugins.modules.FeatureModuleType.FEATURE
import com.everlog.gradleplugins.modules.FeatureModuleType.TEST_FIXTURES
import com.everlog.gradleplugins.modules.FeatureModuleType.UI

/**
 * Why [from] can't depend on [to], or null when it can. [forTests] is true for test configurations
 * (testImplementation, androidTestImplementation and the like). The rules are described in android/README.md.
 */
internal fun moduleRuleBroken(from: Module, to: Module, forTests: Boolean): String? = when {
    to == Module.App -> "nothing depends on the app"
    to.isTestSupport && !forTests && !from.isTestSupport -> "test support is only for tests"
    else -> when (from) {
        Module.App -> null
        Module.DataSync -> dataSyncRuleBroken(to)
        is Module.Common -> commonRuleBroken(to)
        is Module.Feature -> when (from.type) {
            DATA -> dataRuleBroken(to)
            UI -> uiRuleBroken(to)
            FEATURE, TEST_FIXTURES -> featureRuleBroken(from, to)
        }
    }
}

private fun commonRuleBroken(to: Module) = when (to) {
    is Module.Feature -> "common modules never depend on feature modules"
    Module.DataSync -> "common modules never depend on DataSync, which builds on them"
    else -> null
}

private fun dataSyncRuleBroken(to: Module) = when {
    to is Module.Common && !to.isUi -> null
    else -> "DataSync depends only on common modules without UI"
}

private fun dataRuleBroken(to: Module) = when {
    to is Module.Feature && (to.type == FEATURE || to.type == UI) -> "data never depends on UI"
    to is Module.Common && to.isUi -> "data never depends on UI"
    else -> null
}

private fun uiRuleBroken(to: Module) = when {
    to is Module.Feature && to.type == FEATURE -> "a feature's ui module never depends on a feature's screens"
    to == Module.DataSync -> "ui modules read data through a feature's data module, never DataSync"
    else -> null
}

private fun featureRuleBroken(from: Module.Feature, to: Module) = when {
    to is Module.Feature && to.feature != from.feature && to.type == FEATURE -> "a feature never depends on another feature's screens, only on its ui module"
    to == Module.DataSync -> "screens read data through their feature's data module, never DataSync"
    else -> null
}
