package com.everlog.ui.navigator;

import android.net.Uri;

import com.everlog.data.model.ELIntegration;
import com.everlog.data.model.ELRoutine;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.data.model.plan.ELPlan;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.ui.activities.home.congratulate.CongratulateActivity;
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity;
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity;
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import androidx.annotation.NonNull;

public interface Navigator {

    enum ContactType {
        SETS,
        STATISTICS,
        REPORT_PROBLEM,
        FEEDBACK,
        MUSCLE_GOAL,
        RESTORE_PRO,
        INTEGRATION_SYNC,
        INTEGRATION_UNSYNC
    }

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

    void startWorkout(ELRoutine routine,
                      boolean fromRoutine,
                      boolean fromPlan);

    void resumeWorkout(ELWorkout workout);

    void openWorkoutDetails(ELWorkout workout,
                            boolean justFinished,
                            boolean fromPlan);

    void openEditExerciseGroups(@NonNull DefaultCreateExerciseGroupsActivity.Companion.Properties properties);

    void openMuscleGoal();

    void startWorkoutService(ELWorkout workout);

    void stopWorkoutService();

    void notifyWorkoutServiceSetUpdated(ELWorkout workout);

    void notifyWorkoutServiceShowRestTimer(int remainingTimePercent, int remainingTimeSeconds);

    void notifyWorkoutServiceHideRestTimer(ELWorkout workout);

    void openPlayStoreAppDetails();

    void openSetTypePicker(int selectedExercisesCount);

    void sendEmail(ContactType type, Uri logFile);

    void openYoutubeSearch(String query);

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
