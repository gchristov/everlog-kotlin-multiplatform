package com.everlog.ui.navigator;

import androidx.annotation.Nullable;

import com.everlog.config.FeedbackSource;
import com.everlog.android.settings.data.model.ELIntegration;
import com.everlog.android.common.data.model.ELRoutine;
import com.everlog.android.common.data.model.exercise.ELExercise;
import com.everlog.android.plan.data.model.ELPlan;
import com.everlog.android.common.data.model.workout.ELWorkout;
import com.everlog.managers.apprate.RatePromptTrigger;
import com.everlog.services.workout.ShownSet;
import com.everlog.ui.activities.home.congratulate.CongratulateActivity;
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity;
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity;
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import androidx.annotation.NonNull;

public interface Navigator {

    void openLoginWithGoogle(GoogleSignInClient client);

    void openLogin();

    void openResetPassword();

    void openWebView(String url, String title);

    void openUrl(String url);

    void openHome();

    void openRoutinePicker();

    void openEditRoutine(@NonNull CreateRoutineActivity.Companion.Properties properties);

    void openRoutineDetails(ELRoutine routine, boolean viewOnly);

    void openPerformRoutineConfirmation(ELRoutine routine, boolean fromPlan);

    void openExercises();

    void openExercisePicker();

    void openExerciseDetails(@NonNull ExerciseDetailsActivity.Companion.Properties properties);

    void openEditExercise(ELExercise exercise);

    // A new exercise, optionally named already; the result carries its uuid once saved
    void openCreateExercise(@Nullable String name);

    void startWorkout(ELRoutine routine,
                      boolean fromRoutine,
                      boolean fromPlan);

    void resumeWorkout(ELWorkout workout);

    void openWorkoutDetails(ELWorkout workout,
                            boolean justFinished,
                            boolean fromPlan);

    void openEditExerciseGroups(@NonNull DefaultCreateExerciseGroupsActivity.Companion.Properties properties);

    void openMuscleGoal();

    void openDesignSystem();

    void openOnboarding();

    void startWorkoutService(ELWorkout workout);

    // shown is the set the notification shows, or null for the next one not done
    void startWorkoutService(ELWorkout workout, @Nullable ShownSet shown);

    void stopWorkoutService();

    void notifyWorkoutServiceSetUpdated(ELWorkout workout);

    void notifyWorkoutServiceSetUpdated(ELWorkout workout, @Nullable ShownSet shown);

    void notifyWorkoutServiceShowRestTimer(int remainingTimePercent, int remainingTimeSeconds);

    void notifyWorkoutServiceHideRestTimer(ELWorkout workout);

    void openPlayStoreAppDetails();

    /**
     * Asks Play for a rating, {@code delayMillis} from now, if the trigger qualifies and the user is due one.
     */
    void requestRatePrompt(RatePromptTrigger trigger, long delayMillis);

    void openSetTypePicker(int selectedExercisesCount);

    /**
     * Opens the feedback form for {@code source} from Remote Config. Does nothing when there's none, as the
     * entry point is then hidden.
     */
    void openFeedbackForm(FeedbackSource source);

    void runProFeature(Runnable runnable);

    void openProBuy();

    void openProBuyOrManage();

    void openCongratulate(CongratulateActivity.Type type);

    void shareApp();

    void shareWorkout(ELWorkout workout);

    void openCoverImagePicker();

    void openEditPlan(ELPlan plan);

    void openPlanDetails(ELPlan plan);

    void scheduleAppUseNotification();

    void cancelAppUseNotification();

    void promptForAppSettings(int rationaleTextResId);

    void openIntegration(ELIntegration integration);
}
