package com.everlog.ui.activities.questionform

sealed interface Question {
    val id: String
    val title: String

    // What the question starts with before it's answered, e.g. the current setting
    val default: Answer?

    data class YesNo(
        override val id: String,
        override val title: String,
        override val default: Answer.YesNo? = null,
    ) : Question

    data class Number(
        override val id: String,
        override val title: String,
        val min: Double,
        val max: Double,
        val step: Double = 1.0,
        override val default: Answer.Number? = null,
        // Can depend on other answers, e.g. a weight's unit on the chosen weight unit
        val unit: (answers: Map<String, Answer>) -> String? = { null },
    ) : Question

    data class SingleChoice(
        override val id: String,
        override val title: String,
        val options: List<Option>,
        override val default: Answer.SingleChoice? = null,
    ) : Question

    data class MultiChoice(
        override val id: String,
        override val title: String,
        val options: List<Option>,
        override val default: Answer.MultiChoice? = null,
    ) : Question

    data class Option(
        val id: String,
        val label: String,
        val description: String? = null,
        val enabled: Boolean = true,
    )
}

sealed interface Answer {
    data class YesNo(val value: Boolean) : Answer
    data class Number(val value: Double) : Answer
    data class SingleChoice(val optionId: String) : Answer
    data class MultiChoice(val optionIds: Set<String>) : Answer
}
