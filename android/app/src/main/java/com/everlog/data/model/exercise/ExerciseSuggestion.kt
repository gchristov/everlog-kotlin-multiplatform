package com.everlog.data.model.exercise

import org.apache.commons.lang3.text.WordUtils

data class ExerciseSuggestion (

        var name: String? = null

) {
    companion object {

        fun newExerciseSuggestion(name: String): ExerciseSuggestion {
            val suggestion = ExerciseSuggestion()
            // Show exercise with upper case words
            suggestion.name = WordUtils.capitalize(name)
            return suggestion
        }
    }

    fun getFirstChar(): String {
        return name?.get(0).toString()
    }
}