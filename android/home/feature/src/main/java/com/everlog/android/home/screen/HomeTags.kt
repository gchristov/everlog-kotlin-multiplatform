package com.everlog.android.home.screen

object HomeTags {
    const val Button = "home:button"
    const val StartFromTemplate = "home:start:template"
    const val StartEmpty = "home:start:empty"
    const val NewTemplate = "home:new:template"
    const val NewPlan = "home:new:plan"
    const val Resume = "home:prompt:resume"
    const val Discard = "home:prompt:discard"

    fun tab(tab: HomeTab) = "home:tab:${tab.name}"
}
