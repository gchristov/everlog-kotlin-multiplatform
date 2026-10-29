package com.everlog.ui.activities.templateeditor

import android.content.Context
import com.everlog.BuildConfig

/**
 * Prototype: switches to try other layouts of the template editor, from Settings › Developer in debug builds. Read
 * as the editor opens. Release builds always get the defaults.
 */
internal data class TemplateEditorPrototype(
    // The workout's LAST TIME column beside each target
    val lastTime: Boolean = false,
    // One line per exercise, sets × reps, with each set behind Each set
    val setsAndReps: Boolean = false,
    // The name and rest in the header, and shorter rows
    val compact: Boolean = false,
    // Every control on screen: reps beside each set's number, a delete per set, a grip per exercise, links between
    val inline: Boolean = false,
) {
    companion object {
        private const val Prefs = "template_editor_prototype"
        val Labels = arrayOf("Last time column", "Sets × reps layout", "Compact header and rows", "Controls on screen")

        fun read(context: Context): TemplateEditorPrototype {
            if (!BuildConfig.DEBUG) return TemplateEditorPrototype()
            val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
            return TemplateEditorPrototype(prefs.getBoolean("lastTime", false), prefs.getBoolean("setsAndReps", false), prefs.getBoolean("compact", false), prefs.getBoolean("inline", true))
        }

        fun write(context: Context, options: TemplateEditorPrototype) {
            context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit()
                .putBoolean("lastTime", options.lastTime)
                .putBoolean("setsAndReps", options.setsAndReps)
                .putBoolean("compact", options.compact)
                .putBoolean("inline", options.inline)
                .apply()
        }
    }

    fun checked() = booleanArrayOf(lastTime, setsAndReps, compact, inline)
}
