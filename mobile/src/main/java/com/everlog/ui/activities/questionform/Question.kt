package com.everlog.ui.activities.questionform

sealed interface Question {
    val id: String
    val title: String

    data class YesNo(
        override val id: String,
        override val title: String,
    ) : Question

    data class Number(
        override val id: String,
        override val title: String,
        val min: Int,
        val max: Int,
        val unit: String? = null,
    ) : Question

    data class SingleChoice(
        override val id: String,
        override val title: String,
        val options: List<Option>,
    ) : Question

    data class MultiChoice(
        override val id: String,
        override val title: String,
        val options: List<Option>,
    ) : Question

    data class Option(
        val id: String,
        val label: String,
    )
}

sealed interface Answer {
    data class YesNo(val value: Boolean) : Answer
    data class Number(val value: Int) : Answer
    data class SingleChoice(val optionId: String) : Answer
    data class MultiChoice(val optionIds: Set<String>) : Answer
}

// Placeholder questions for the prototype. Real ones will come from string resources or config.
internal val SampleQuestions = listOf(
    Question.YesNo(
        id = "experience",
        title = "Have you trained with weights before?",
    ),
    Question.Number(
        id = "days",
        title = "How many days a week can you train?",
        min = 1,
        max = 7,
        unit = "days",
    ),
    Question.SingleChoice(
        id = "goal",
        title = "What's your main goal?",
        options = listOf(
            Question.Option(id = "muscle", label = "Build muscle"),
            Question.Option(id = "strength", label = "Get stronger"),
            Question.Option(id = "fat", label = "Lose fat"),
            Question.Option(id = "health", label = "Stay healthy"),
        ),
    ),
    Question.MultiChoice(
        id = "equipment",
        title = "Which equipment can you use?",
        options = listOf(
            Question.Option(id = "barbell", label = "Barbell"),
            Question.Option(id = "dumbbells", label = "Dumbbells"),
            Question.Option(id = "machines", label = "Machines"),
            Question.Option(id = "bodyweight", label = "Bodyweight only"),
        ),
    ),
    Question.Number(
        id = "duration",
        title = "How long is a typical session?",
        min = 10,
        max = 180,
        unit = "minutes",
    ),
)
