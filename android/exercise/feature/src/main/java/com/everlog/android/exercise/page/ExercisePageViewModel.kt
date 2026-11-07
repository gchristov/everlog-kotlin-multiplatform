package com.everlog.android.exercise.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.exercise.data.DeleteExerciseUseCase
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.exercisehistory.ExerciseHistory
import com.everlog.android.workout.data.exercisehistory.ExerciseHistoryRead
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsPreferences
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import com.everlog.android.workout.data.exercisehistory.exerciseStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate
import timber.log.Timber

/**
 * An exercise's page: its Info, Statistics and History. The statistics and the history's last 12 months follow the
 * cache, so a workout saved counts straight away, and older years load a year at a time as the history scrolls back.
 * A user's own exercise edited while the page is open shows the edit. Statistics open on the range last picked for
 * any exercise. Settings and the date are read again on [onResume].
 *
 * @param exercise As the page was opened with it
 * @param userId The signed-in user's, whose own exercises they can edit and delete
 * @param readHistory The exercise's history by its uuid (WorkoutRepository.exerciseHistory)
 * @param ownExerciseUpdates The user's own exercises as they change
 * @param exampleNames The workouts the example's sessions are in
 */
class ExercisePageViewModel(
    dispatcher: CoroutineDispatcher,
    exercise: ELExercise,
    userId: String?,
    private val readHistory: (String) -> ExerciseHistoryRead,
    private val loadGuide: LoadExerciseGuideUseCase,
    private val ownExerciseUpdates: () -> Flow<List<LibraryExercise>>,
    private val deleteExercise: DeleteExerciseUseCase,
    private val settings: UserSettings,
    private val preferences: ExerciseStatsPreferences,
    private val analytics: ExercisePageAnalytics,
    private val feedback: () -> Boolean,
    private val exampleNames: List<String>,
    private val today: () -> LocalDate,
) : CommonViewModel<ExercisePageState>(dispatcher, initialState(exercise, exercise.isEditable(userId), preferences.range(), settings, today(), feedback())) {

    private val uuid = exercise.uuid.orEmpty()
    private var history: ExerciseHistoryRead? = null
    private var historyJob: Job? = null

    init {
        read()
        if (state.value.own) followOwnExercise() else loadGuide()
    }

    fun onResume() {
        analytics.screenViewed()
        setState { copy(weightUnit = settings.weightUnit(), feedback = feedback()) }
        show()
    }

    fun onTabShown(tab: ExercisePageTab) = analytics.tabViewed(tab)

    fun onRangePicked(range: ExerciseStatsRange) {
        if (range == state.value.range) return
        analytics.rangePicked(range)
        preferences.setRange(range)
        setState { copy(range = range) }
        show()
    }

    // The history scrolled to its oldest session: the year before, unless there's nothing older. Not after a year
    // failed, which only Try again reads again, so the list doesn't keep retrying by itself.
    fun onHistoryEnd() {
        val state = state.value
        if (!state.loading && !state.complete && !state.loadingMore && !state.olderFailed) history?.loadMore()
    }

    fun onTryAgain() = read()

    // An older year that couldn't be read, read again
    fun onTryOlderAgain() {
        if (state.value.olderFailed) history?.loadMore()
    }

    fun onDeleteClicked() = setState { copy(deletePrompt = true) }

    fun onDeleteCancelled() = setState { copy(deletePrompt = false) }

    fun onDeleteConfirmed() {
        setState { copy(deletePrompt = false) }
        launchCoroutine {
            deleteExercise(state.value.exercise).fold(
                { error ->
                    Timber.tag(TAG).w("Couldn't delete %s: %s", uuid, error.message)
                    setState { copy(deleteFailed = true) }
                },
                {
                    analytics.exerciseDeleted()
                    setState { copy(closed = true) }
                },
            )
        }
    }

    fun onDeleteFailedShown() = setState { copy(deleteFailed = false) }

    // A new read, e.g. to try again: from the last 12 months, scrolled back to the start
    private fun read() {
        historyJob?.cancel()
        val read = readHistory(uuid)
        history = read
        setState { copy(loading = true, failed = false) }
        historyJob = launchCoroutine {
            read.loads.collect { load ->
                when (load) {
                    is Load.Ready -> {
                        setState { withHistory(load.value) }
                        // Nothing to show yet: read further back until a session, the first workout or a failure
                        load.value.run { if (sessions.isEmpty() && !loadingMore && !complete && !failed) read.loadMore() }
                    }
                    is Load.Unavailable -> setState { copy(loading = false, failed = true) }
                    Load.Loading -> Unit
                }
                show()
            }
        }
    }

    private fun loadGuide() = launchCoroutine {
        loadGuide(uuid).collect { load ->
            when (load) {
                is Load.Ready -> setState { copy(guide = load.value) }
                is Load.Unavailable -> Timber.tag(TAG).w("Couldn't load the guide for %s: %s", uuid, load.error.message)
                Load.Loading -> Unit
            }
        }
    }

    // A rename or new image, e.g. from Edit, shows straight away. One deleted elsewhere keeps showing as it was.
    private fun followOwnExercise() = launchCoroutine {
        ownExerciseUpdates().collect { exercises ->
            exercises.firstOrNull { it.uuid == uuid }?.let { updated ->
                setState { copy(exercise = exercise.copy(name = updated.name, imageUrl = updated.imageUrl, category = updated.category)) }
            }
        }
    }

    private fun ExercisePageState.withHistory(history: ExerciseHistory) = copy(
        sessions = history.sessions,
        loading = false,
        failed = false,
        loadingMore = history.loadingMore,
        complete = history.complete,
        olderFailed = history.failed,
    )

    private fun show() {
        val date = today()
        setState {
            val stats = exerciseStats(sessions, range, date, settings.firstDayOfWeek(), weightUnit)
            copy(
                today = date,
                stats = stats,
                statsExample = if (!loading && stats.timesPerformed == 0) exampleStats(range, date) else null,
                // As on the Activity tab: once there turn out to be none, or when they couldn't load
                historyExample = if (sessions.isEmpty() && (complete || failed || olderFailed)) exampleSessions(date, exampleNames) else null,
            )
        }
    }

    private fun ExercisePageState.exampleStats(range: ExerciseStatsRange, date: LocalDate) =
        exerciseStats(exampleSessions(date, exampleNames), range, date, settings.firstDayOfWeek(), weightUnit)

    private companion object {
        const val TAG = "ExercisePage"

        fun initialState(exercise: ELExercise, own: Boolean, range: ExerciseStatsRange, settings: UserSettings, today: LocalDate, feedback: Boolean) = ExercisePageState(
            exercise = exercise,
            own = own,
            range = range,
            today = today,
            weightUnit = settings.weightUnit(),
            stats = exerciseStats(emptyList(), range, today, settings.firstDayOfWeek(), settings.weightUnit()),
            feedback = feedback,
        )
    }
}
