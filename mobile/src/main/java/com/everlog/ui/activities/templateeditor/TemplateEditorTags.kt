package com.everlog.ui.activities.templateeditor

/**
 * The template editor's test tags. An exercise is tagged by its place in the list, a set by its exercise, its round
 * and its exercise in the round (always 0 outside a superset or circuit).
 */
internal object TemplateEditorTags {
    const val Keypad = "keypad"
    const val Save = "save"
    const val Name = "settings:name"
    const val NameField = "name_field"
    const val Rest = "settings:rest"
    const val TimeForAllSets = "time_for_all_sets"
    const val List = "exercises"
    const val Example = "example"

    fun exercise(index: Int) = "exercise:$index"

    fun grip(index: Int) = "exercise:$index:grip"

    fun addSet(index: Int) = "exercise:$index:add_set"

    fun join(index: Int) = "exercise:$index:join"

    fun split(index: Int) = "exercise:$index:split"

    fun set(index: Int, round: Int, exercise: Int) = "exercise:$index:set:$round:$exercise"

    fun value(index: Int, round: Int, exercise: Int) = "${set(index, round, exercise)}:value"

    fun deleteSet(index: Int, round: Int, exercise: Int) = "${set(index, round, exercise)}:delete"
}
