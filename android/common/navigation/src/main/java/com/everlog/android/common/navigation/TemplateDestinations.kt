package com.everlog.android.common.navigation

import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.set.ELSetType

data class TemplateDetails(val routine: ELRoutine, val viewOnly: Boolean) : Destination

// The template editor for [routine], or for a new template when null. Hands back the saved template.
data class TemplateEditor(
    val routine: ELRoutine? = null,
    // Off when the screen that opened it decides what comes next
    val showDetailsOnSuccess: Boolean = true,
) : ResultDestination<ELRoutine> {
    override val screenResult get() = Companion

    companion object : ScreenResult<ELRoutine> {
        override val requestCode = 6

        override fun read(data: Intent?): ELRoutine? = data.serializable(NavigationExtras.ROUTINE)
    }
}

data object TemplatePicker : ResultDestination<ELRoutine>, ScreenResult<ELRoutine> {
    override val screenResult get() = this

    override val requestCode = 2

    override fun read(data: Intent?): ELRoutine? = data.serializable(NavigationExtras.ROUTINE)
}

// The set type for [exerciseCount] exercises: some types need more than one
data class SetTypePicker(val exerciseCount: Int) : ResultDestination<ELSetType> {
    override val screenResult get() = Companion

    companion object : ScreenResult<ELSetType> {
        override val requestCode = 3

        override fun read(data: Intent?): ELSetType? = data.serializable(NavigationExtras.SET_TYPE)
    }
}
