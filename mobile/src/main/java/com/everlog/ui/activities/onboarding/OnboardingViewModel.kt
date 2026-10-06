package com.everlog.ui.activities.onboarding

import com.everlog.data.controllers.starterroutines.BuildStarterRoutinesUseCase
import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.DayOfWeek
import timber.log.Timber

/**
 * The welcome, then the setup questionnaire from the Everlog Onboarding design: one question open
 * at a time, answered questions collapse to summary rows that can be reopened, then Build my
 * week builds the starter routines and finishes.
 *
 * A prototype for now: answers aren't saved, and the starter routines are built but only logged,
 * not saved or shown.
 */
class OnboardingViewModel(
    dispatcher: CoroutineDispatcher,
    private val buildStarterRoutinesUseCase: BuildStarterRoutinesUseCase,
    private val now: () -> Long = System::currentTimeMillis,
    questions: List<OnboardingQuestion> = OnboardingQuestions.all,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) : CommonViewModel<OnboardingViewModel.State>(
    dispatcher = dispatcher,
    initialState = State(questions = questions, activeQuestionId = null, firstDayOfWeek = firstDayOfWeek).activate(questions.firstOrNull()?.id)
) {
    // Let's go on the welcome
    fun onStart() {
        setState { copy(step = Step.Questions) }
    }

    fun onOptionSelect(questionId: String, optionId: String) {
        updateInput(questionId) { Answer.Choice(optionId) }
    }

    fun onDaysSelect(questionId: String, count: Int) {
        updateInput(questionId) { Answer.Days(count) }
    }

    fun onReminderDayToggle(questionId: String, day: DayOfWeek) {
        updateInput(questionId) { input ->
            val reminders = input as? Answer.Reminders ?: return@updateInput input
            reminders.copy(days = if (day in reminders.days) reminders.days - day else reminders.days + day)
        }
    }

    fun onReminderTimeChange(questionId: String, hour: Int, minute: Int) {
        updateInput(questionId) { input ->
            (input as? Answer.Reminders)?.copy(hour = hour, minute = minute) ?: input
        }
    }

    // Next, Done (when editing) and Remind me
    fun onSubmit() {
        val currentState = state.value
        val questionId = currentState.activeQuestionId ?: return
        val answer = currentState.input ?: return
        if (!currentState.canContinue) return
        setState { answer(questionId, answer, skipped = false) }
    }

    fun onSkip() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, OnboardingQuestions.skippedAnswer(questionId), skipped = true) }
    }

    fun onNotNow() {
        val questionId = state.value.activeQuestionId ?: return
        setState { answer(questionId, Answer.RemindersOff, skipped = false) }
    }

    fun onEdit(questionId: String) {
        if (questionId !in state.value.answers) return
        setState { activate(questionId) }
    }

    // Leaves an edit without changing the answer
    fun onCancel() {
        setState { activate(firstUnansweredId) }
    }

    fun onBuild() {
        val currentState = state.value
        if (!currentState.allAnswered || currentState.step == Step.Building) return
        setState { copy(step = Step.Building) }
        launchCoroutine {
            val profile = OnboardingQuestions.starterProfile(currentState.answers)
            buildStarterRoutinesUseCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = now())).fold(
                // Reported as a non-fatal. For now the screen closes and says so; retry or skip come later.
                ifLeft = {
                    Timber.tag(TAG).e(it)
                    setState { copy(finished = true, buildFailed = true) }
                },
                ifRight = { routines ->
                    Timber.tag(TAG).i("Built starter routines for %s: %s", profile, routines.joinToString { "${it.name} (${it.getTotalExercises()} exercises)" })
                    setState { copy(finished = true) }
                },
            )
        }
    }

    private fun updateInput(questionId: String, update: (Answer?) -> Answer?) {
        if (questionId != state.value.activeQuestionId) return
        setState { copy(input = update(input)) }
    }

    // What the screen shows, in order
    enum class Step {
        // Until Let's go
        Welcome,
        // One question open at a time, until Build my week
        Questions,
        // The starter routines building, until the screen closes
        Building,
    }

    data class State(
        val questions: List<OnboardingQuestion>,
        val step: Step = Step.Welcome,
        val answers: Map<String, Answer> = emptyMap(),
        // Questions skipped, whose answers are the defaults
        val skipped: Set<String> = emptySet(),
        // The open question: the next one to answer, or an answered one being edited
        val activeQuestionId: String?,
        // In-progress answer for the open question
        val input: Answer? = null,
        // Building the starter routines is over, so the screen closes
        val finished: Boolean = false,
        // The starter routines couldn't be built, which the screen says as it closes
        val buildFailed: Boolean = false,
        // From Settings, for the order of the reminder days
        val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ) {
        val firstUnansweredId: String? get() = questions.firstOrNull { it.id !in answers }?.id

        val activeQuestion: OnboardingQuestion? get() = questions.firstOrNull { it.id == activeQuestionId }

        val isEditing: Boolean get() = activeQuestionId != null && activeQuestionId in answers

        // Every question up to the next one to answer. Later ones stay hidden.
        val visibleQuestions: List<OnboardingQuestion>
            get() {
                val firstUnanswered = questions.indexOfFirst { it.id !in answers }
                return if (firstUnanswered == -1) questions else questions.take(firstUnanswered + 1)
            }

        val allAnswered: Boolean get() = answers.size == questions.size

        val canContinue: Boolean
            get() = when (val input = input) {
                is Answer.Reminders -> input.days.isNotEmpty()
                null -> false
                else -> true
            }

        internal fun answer(questionId: String, answer: Answer, skipped: Boolean): State {
            val newState = copy(
                answers = answers + (questionId to answer),
                skipped = if (skipped) this.skipped + questionId else this.skipped - questionId,
            )
            return newState.activate(newState.firstUnansweredId)
        }

        // Opens a question, starting from its answer, or what the design preselects
        internal fun activate(questionId: String?): State {
            val question = questions.firstOrNull { it.id == questionId }
            // A skipped question or reminders turned off start again from the preselection
            val answer = answers[questionId]?.takeIf { questionId !in skipped && it != Answer.RemindersOff }
            return copy(
                activeQuestionId = questionId,
                input = answer ?: question?.let { initialInput(it) },
            )
        }

        private fun initialInput(question: OnboardingQuestion): Answer? = when (question.id) {
            // Preselected from the device's locale
            OnboardingQuestions.Units -> Answer.Choice(OnboardingQuestions.deviceUnit())
            // Days follow the days-a-week answer, at 18:00
            OnboardingQuestions.Reminders -> {
                val days = (answers[OnboardingQuestions.Days] as? Answer.Days)?.count ?: 3
                Answer.Reminders(OnboardingQuestions.trainingDays(days).toSet(), hour = 18, minute = 0)
            }
            else -> null
        }
    }

    private companion object {
        const val TAG = "OnboardingViewModel"
    }
}
