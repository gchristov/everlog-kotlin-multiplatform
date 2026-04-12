package com.everlog.ui.navigator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.everlog.BuildConfig;
import com.everlog.R;
import com.everlog.constants.ELActivityRequestCodes;
import com.everlog.constants.ELConstants;
import com.everlog.data.model.ELIntegration;
import com.everlog.data.model.ELRoutine;
import com.everlog.data.model.ELUser;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.data.model.plan.ELPlan;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.managers.AppUsageReminderManager;
import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.auth.LocalUserManager;
import com.everlog.managers.preferences.SettingsManager;
import com.everlog.services.workout.WorkoutService;
import com.everlog.ui.activities.home.HomeActivity;
import com.everlog.ui.activities.home.congratulate.CongratulateActivity;
import com.everlog.ui.activities.home.cover.CoverImagePickerActivity;
import com.everlog.ui.activities.home.exercise.ExercisesActivity;
import com.everlog.ui.activities.home.exercise.create.CreateExerciseActivity;
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity;
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity;
import com.everlog.ui.activities.home.integration.IntegrationActivity;
import com.everlog.ui.activities.home.musclegoal.MuscleGoalActivity;
import com.everlog.ui.activities.home.plan.create.CreatePlanActivity;
import com.everlog.ui.activities.home.plan.details.PlanDetailsActivity;
import com.everlog.ui.activities.home.pro.ProActivity;
import com.everlog.ui.activities.home.routine.RoutinePickerActivity;
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity;
import com.everlog.ui.activities.home.routine.details.RoutineDetailsActivity;
import com.everlog.ui.activities.home.routine.perform.PerformRoutineActivity;
import com.everlog.ui.activities.home.settype.SetTypePickerActivity;
import com.everlog.ui.activities.home.web.WebViewActivity;
import com.everlog.ui.activities.home.workout.WorkoutActivity;
import com.everlog.ui.activities.home.workout.details.WorkoutDetailsActivity;
import com.everlog.ui.activities.login.LoginActivity;
import com.everlog.ui.activities.login.resetpassword.ResetPasswordActivity;
import com.everlog.ui.dialog.DialogBuilder;
import com.everlog.ui.dialog.ToastBuilder;
import com.everlog.utils.ViewUtils;
import com.everlog.utils.device.DeviceInfo;
import com.everlog.utils.format.StatsFormatUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.imagepick.client.share.ELImageShare;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import rx.Observable;
import timber.log.Timber;

import static com.everlog.constants.ELConstants.EXTRA_EXERCISE;
import static com.everlog.constants.ELConstants.EXTRA_PLAN_UUID;
import static com.everlog.constants.ELConstants.EXTRA_ROUTINE;
import static com.everlog.constants.ELConstants.EXTRA_VIEW_ONLY;
import static com.everlog.constants.ELConstants.EXTRA_WEB_TITLE;
import static com.everlog.constants.ELConstants.EXTRA_WEB_URL;
import static com.everlog.constants.ELConstants.EXTRA_WORKOUT;
import static com.everlog.constants.ELConstants.EXTRA_WORKOUT_FROM_PLAN;
import static com.everlog.constants.ELConstants.EXTRA_WORKOUT_JUST_FINISHED;

public class ELNavigator implements Navigator {

    private static final String TAG = "ELNavigator";

    private static final String URL_YOUTUBE = "https://www.youtube.com/results?search_query=%s";
    private static final String URL_PLAY_STORE = "https://play.google.com/store/apps/details?id=%s";
    private static final String URL_RATE = "market://details?id=%s";
    private static final String URL_SUBSCRIPTION = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";

    private static final String APP_PACKAGE = "com.everlog";
    private static final String INTENT_TYPE_EMAIL = "message/rfc822";
    private static final String INTENT_TYPE_TEXT = "text/plain";

    private static final String SHARE_APP_TEXT = "Track and plan your workouts like a pro with Everlog!\n\n%s";
    private static final String SHARE_WORKOUT_TEXT = "I have just completed a workout with Everlog!\n\nGet the app here - %s";

    private Context mContext;

    public ELNavigator(Context context) {
        this.mContext = context;
    }

    @Override
    public void openLoginWithGoogle(GoogleSignInClient client) {
        Intent intent = client.getSignInIntent();
        startActivityForResult(intent, ELActivityRequestCodes.REQUEST_LOGIN_GOOGLE);
    }

    @Override
    public void openLogin() {
        Intent i = new Intent(mContext, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        closeCurrentActivity();
    }

    @Override
    public void openWebView(String url, String title) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra(EXTRA_WEB_URL, url);
        intent.putExtra(EXTRA_WEB_TITLE, title);
        startActivity(intent);
    }

    @Override
    public void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void openHome() {
        Intent i = new Intent(mContext, HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        closeCurrentActivity();
    }

    @Override
    public void openRoutinePicker() {
        Intent i = new Intent(mContext, RoutinePickerActivity.class);
        startActivityForResult(i, ELActivityRequestCodes.REQUEST_PICK_ROUTINE);
    }

    @Override
    public void openExercises() {
        startActivity(ExercisesActivity.launchIntent(mContext, new ExercisesActivity.Companion.Properties()));
    }

    @Override
    public void openExercisePicker() {
        startActivityForResult(ExercisesActivity.launchIntent(mContext, new ExercisesActivity.Companion.Properties()
                .selection(true)), ELActivityRequestCodes.REQUEST_PICK_EXERCISES);
    }

    @Override
    public void openEditExercise(ELExercise exercise) {
        Intent intent = new Intent(mContext, CreateExerciseActivity.class);
        if (exercise != null) {
            intent.putExtra(EXTRA_EXERCISE, exercise);
        }
        startActivityForResult(intent, ELActivityRequestCodes.REQUEST_EDIT_EXERCISE);
    }

    @Override
    public void openExerciseDetails(ExerciseDetailsActivity.Companion.@NotNull Properties properties) {
        startActivity(ExerciseDetailsActivity.launchIntent(mContext, properties));
    }

    @Override
    public void openEditRoutine(CreateRoutineActivity.Companion.@NotNull Properties properties) {
        startActivityForResult(CreateRoutineActivity.launchIntent(mContext, properties), ELActivityRequestCodes.REQUEST_EDIT_ROUTINE);
    }

    @Override
    public void openRoutineDetails(ELRoutine routine, boolean viewOnly) {
        Intent intent = new Intent(mContext, RoutineDetailsActivity.class);
        intent.putExtra(EXTRA_ROUTINE, routine);
        intent.putExtra(EXTRA_VIEW_ONLY, viewOnly);
        startActivity(intent);
    }

    @Override
    public void openPerformRoutineConfirmation(ELRoutine routine, boolean fromPlan) {
        Intent intent = new Intent(mContext, PerformRoutineActivity.class);
        intent.putExtra(EXTRA_ROUTINE, routine);
        intent.putExtra(EXTRA_WORKOUT_FROM_PLAN, fromPlan);
        startActivity(intent);
    }

    @Override
    public void startWorkout(ELRoutine routine,
                             boolean fromRoutine,
                             boolean fromPlan) {
        AnalyticsManager.manager.workoutStarted();
        if (fromRoutine) {
            AnalyticsManager.manager.workoutFromRoutineStarted();
        } else {
            AnalyticsManager.manager.workoutQuickStarted();
        }
        // Build workout from routine.
        ELWorkout workout = ELWorkout.getWorkoutFromRoutine(routine, fromRoutine);
        // Start workout.
        Intent intent = new Intent(mContext, WorkoutActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_WORKOUT, workout);
        intent.putExtra(EXTRA_WORKOUT_FROM_PLAN, fromPlan);
        startActivity(intent);
    }

    @Override
    public void resumeWorkout(ELWorkout workout) {
        Intent intent = new Intent(mContext, WorkoutActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_WORKOUT, workout);
        startActivity(intent);
    }

    @Override
    public void openWorkoutDetails(ELWorkout workout,
                                   boolean justFinished,
                                   boolean fromPlan) {
        Intent intent = new Intent(mContext, WorkoutDetailsActivity.class);
        intent.putExtra(EXTRA_WORKOUT, workout);
        intent.putExtra(EXTRA_WORKOUT_JUST_FINISHED, justFinished);
        intent.putExtra(EXTRA_WORKOUT_FROM_PLAN, fromPlan);
        startActivity(intent);
    }

    @Override
    public void openEditExerciseGroups(DefaultCreateExerciseGroupsActivity.Companion.@NotNull Properties properties) {
        startActivityForResult(DefaultCreateExerciseGroupsActivity.launchIntent(mContext, properties), ELActivityRequestCodes.REQUEST_EDIT_EXERCISE_GROUPS);
    }

    @Override
    public void openMuscleGoal() {
        Intent i = new Intent(mContext, MuscleGoalActivity.class);
        startActivity(i);
    }

    @Override
    public void startWorkoutService(ELWorkout workout) {
        Intent i = new Intent(mContext, WorkoutService.class);
        i.setAction(WorkoutService.ACTION_SERVICE_START);
        i.putExtra(ELConstants.EXTRA_WORKOUT, workout);
        startService(i);
    }

    @Override
    public void stopWorkoutService() {
        Intent i = new Intent(mContext, WorkoutService.class);
        i.setAction(WorkoutService.ACTION_SERVICE_STOP);
        startService(i);
    }

    @Override
    public void notifyWorkoutServiceSetUpdated(ELWorkout workout) {
        Intent i = new Intent(WorkoutService.BROADCAST_SERVICE_SET_UPDATED);
        i.putExtra(ELConstants.EXTRA_WORKOUT, workout);
        sendBroadcast(i);
    }

    @Override
    public void notifyWorkoutServiceShowRestTimer(int remainingTimePercent, int remainingTimeSeconds) {
        Intent i = new Intent(WorkoutService.BROADCAST_SERVICE_SHOW_REST_TIMER);
        i.putExtra(ELConstants.EXTRA_REST_TIMER_PROGRESS, remainingTimePercent);
        i.putExtra(ELConstants.EXTRA_REST_TIMER_REMAINING_SECONDS, remainingTimeSeconds);
        sendBroadcast(i);
    }

    @Override
    public void notifyWorkoutServiceHideRestTimer(ELWorkout workout) {
        // WorkoutService handles this already, so no need for an extra state.
        notifyWorkoutServiceSetUpdated(workout);
    }

    @Override
    public void openPlayStoreAppDetails() {
        Uri uri = Uri.parse(String.format(URL_RATE, APP_PACKAGE));
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            ToastBuilder.showToast(mContext, R.string.error_play_store);
        }
    }

    @Override
    public void openSetTypePicker(int selectedExercisesCount) {
        Intent i = new Intent(mContext, SetTypePickerActivity.class);
        i.putExtra(ELConstants.EXTRA_NUMBER_OF_ITEMS, selectedExercisesCount);
        startActivityForResult(i, ELActivityRequestCodes.REQUEST_PICK_SET_TYPE);
    }

    @Override
    public void sendEmail(ContactType type, Uri logFile) {
        String title = "Send mail";
        String subject = "Hello";
        String body = "I would like to leave the following feedback:";
        switch (type) {
            case REPORT_PROBLEM:
                subject = "Report Problem";
                body = "I have the following problem with the app:";
                break;
            case SETS:
                subject = "Sets Request";
                body = "I would like the following set types:";
                break;
            case STATISTICS:
                subject = "Statistics Request";
                body = "I would like the following statistics:";
                break;
            case FEEDBACK:
                subject = "Feedback Request";
                body = "I would like to leave the following feedback:";
                break;
            case MUSCLE_GOAL:
                subject = "Muscle Goal Request";
                body = "I would like the following muscle goals:";
                break;
            case RESTORE_PRO:
                subject = "Restore Membership";
                body = "I would like to restore my membership.";
                break;
            case INTEGRATION_SYNC:
                subject = "Integration Sync Request";
                body = "I would like to sync my existing history with my integrations.";
                break;
            case INTEGRATION_UNSYNC:
                subject = "Integration Un-sync Request";
                body = "I would like to un-sync my existing history with my integrations.";
                break;
        }
        String textBody = body
                .concat("\n\n\n")
                .concat("Account: ").concat(LocalUserManager.getUser().getId())
                .concat("\n\n")
                .concat("Device: ").concat(new DeviceInfo.Builder().build().getDeviceInfo()).concat("\n");
        Intent intent = ShareCompat.IntentBuilder.from((Activity) mContext)
                .setStream(logFile)
                .setType(INTENT_TYPE_EMAIL)
                .getIntent()
                .setAction(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_EMAIL, new String[]{ELConstants.SUPPORT_EMAIL})
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, textBody);
        startActivity(Intent.createChooser(intent, title));
    }

    @Override
    public void openYoutubeSearch(String query) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(URL_YOUTUBE, query)));
        startActivity(intent);
    }

    @Override
    public void runProFeature(Runnable runnable) {
        ELUser user = LocalUserManager.getUser();
        if (user == null) {
            ToastBuilder.showToast(mContext, "An unexpected error occurred. We've been notified and taking a look. Feel free to try again.", true);
            Timber.tag(TAG).e(new RuntimeException("Trying to access pro feature without user account"));
            return;
        }
        if (user.isPro()) {
            runnable.run();
        } else {
            // User is not Pro, so ask them to upgrade
            openProBuy();
        }
    }

    @Override
    public void openProBuy() {
        Intent i = new Intent(mContext, ProActivity.class);
        startActivity(i);
    }

    @Override
    public void openProBuyOrManage() {
        ELUser user = LocalUserManager.getUser();
        if (user.isPro()) {
            AnalyticsManager.manager.proManageSubscription();
            // Show subscription manager in the Play Store
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(URL_SUBSCRIPTION, user.getSubscription().getSku(), APP_PACKAGE)));
            startActivity(browserIntent);
        } else {
            openProBuy();
        }
    }

    @Override
    public void openCongratulate(CongratulateActivity.Type type) {
        startActivity(CongratulateActivity.launchIntent(mContext, new CongratulateActivity.Companion.Properties()
                .type(type)));
    }

    @Override
    public void shareApp() {
        String url = String.format(URL_PLAY_STORE, APP_PACKAGE);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(SHARE_APP_TEXT, url));
        sendIntent.setType(INTENT_TYPE_TEXT);
        startActivity(sendIntent);
    }

    @Override
    public void shareWorkout(ELWorkout workout) {
        String url = String.format(URL_PLAY_STORE, APP_PACKAGE);
        int dp = ViewUtils.dpToPxFromRaw(mContext, R.dimen.share_workout_size);
        View v = View.inflate(mContext, R.layout.view_share_workout, null);
        // Fill view data.
        TextView title = v.findViewById(R.id.nameField);
        TextView sets = v.findViewById(R.id.setsField);
        TextView weight = v.findViewById(R.id.weightField);
        TextView duration = v.findViewById(R.id.timeField);
        TextView exercises = v.findViewById(R.id.exercisesField);
        title.setText(workout.getName());
        sets.setText(StatsFormatUtils.Companion.formatNumberStatsLabel(workout.getTotalSets()));
        weight.setText(String.format("%s %s", StatsFormatUtils.Companion.formatWeightStatsLabel(workout.getTotalWeight()), SettingsManager.weightUnitAbbreviation()));
        duration.setText(String.format("%s %s", StatsFormatUtils.Companion.formatTimeStatsLabel(workout.getDurationMillis()), mContext.getString(R.string.hour)));
        exercises.setText(workout.getTotalExercises() + "");
        // Draw everything.
        v.measure(View.MeasureSpec.makeMeasureSpec(dp, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        // Capture screen.
        Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        // Share
        ELImageShare
                .withActivity((Activity) mContext)
                .withBitmap(b)
                .withCaption(String.format(SHARE_WORKOUT_TEXT, url))
                .withShareErrorListener(throwable -> {
                    Timber.tag(TAG).e(throwable);
                    ToastBuilder.showToast(mContext, "Couldn't share workout: " + throwable.getMessage());
                })
                .share();
    }

    @Override
    public void openCoverImagePicker() {
        Intent intent = new Intent(mContext, CoverImagePickerActivity.class);
        startActivityForResult(intent, ELActivityRequestCodes.REQUEST_PICK_COVER_IMAGE);
    }

    @Override
    public void openEditPlan(ELPlan plan) {
        Intent intent = new Intent(mContext, CreatePlanActivity.class);
        if (plan != null) {
            intent.putExtra(EXTRA_PLAN_UUID, plan.getUuid());
        }
        startActivity(intent);
    }

    @Override
    public void openPlanDetails(ELPlan plan) {
        Intent intent = new Intent(mContext, PlanDetailsActivity.class);
        intent.putExtra(EXTRA_PLAN_UUID, plan.getUuid());
        startActivity(intent);
    }

    @Override
    public void scheduleAppUseNotification() {
        AppUsageReminderManager.schedule();
    }

    @Override
    public void cancelAppUseNotification() {
        AppUsageReminderManager.cancel();
    }

    @Override
    public void promptForAppSettings(int rationaleTextResId) {
        Observable.just(DialogBuilder.showPrompt(mContext,
                mContext.getString(R.string.everlog_app_name),
                mContext.getString(rationaleTextResId),
                mContext.getString(R.string.settings_permission_grant),
                mContext.getString(R.string.cancel))
                .subscribe(integer -> {
                    if (integer == DialogInterface.BUTTON_POSITIVE) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }));
    }

    @Override
    public void openIntegration(ELIntegration integration) {
        startActivity(IntegrationActivity.Companion.launchIntent(mContext, new IntegrationActivity.Companion.Properties()
                .integration(integration)));
    }

    @Override
    public void openResetPassword() {
        Intent intent = new Intent(mContext, ResetPasswordActivity.class);
        startActivity(intent);
    }

    // Utils

    private void sendBroadcast(Intent i) {
        if (mContext != null) {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
        }
    }

    private void startService(Intent intent) {
        if (mContext != null) {
            mContext.startService(intent);
        }
    }

    private void startActivity(Intent intent) {
        if (mContext != null) {
            mContext.startActivity(intent);
        }
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        if (mContext != null && mContext instanceof Activity) {
            ((Activity) mContext).startActivityForResult(intent, requestCode);
        }
    }

    private void closeCurrentActivity() {
        if (mContext != null && mContext instanceof Activity) {
            ((Activity) mContext).finish();
        }
    }

    private void showBottomSheet(BottomSheetDialogFragment fragment) {
        if (mContext != null && mContext instanceof AppCompatActivity) {
            fragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), fragment.getTag());
        }
    }
}
