package com.everlog.ui.activities.home.workout.details;

import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;

import com.ahamed.multiviewadapter.DataListManager;
import com.ahamed.multiviewadapter.RecyclerAdapter;
import com.everlog.R;
import com.everlog.android.common.kotlin.DateExtKt;
import com.everlog.android.settings.data.SettingsManager;
import com.everlog.constants.ELActivityRequestCodes;
import com.everlog.data.controllers.statistics.UserStatsController;
import com.everlog.data.datastores.ELDatastore;
import com.everlog.android.common.data.store.OnStoreItemsListener;
import com.everlog.data.datastores.history.ELDocStoreWorkoutLoadedEvent;
import com.everlog.android.common.data.model.ELRoutine;
import com.everlog.android.common.data.model.exercise.ELExerciseGroup;
import com.everlog.android.common.data.model.workout.ELWorkout;
import com.everlog.android.common.data.model.workout.RangeType;
import com.everlog.managers.PlanManager;
import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.apprate.RatePromptTrigger;
import com.everlog.android.common.mvp.activity.BaseActivityPresenter;
import com.everlog.android.common.kotlin.Utils;
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity;
import com.everlog.ui.adapters.WorkoutDetailsStatsAdapter;
import com.everlog.android.common.mvp.list.ExerciseGroupSummaryAdapter;
import com.everlog.android.exercise.feature.details.ExerciseDetailsActivity;
import com.everlog.ui.dialog.AppDialogs;
import com.everlog.ui.navigator.ELNavigator;
import com.everlog.ui.navigator.PresenterNavigator;
import com.google.firebase.firestore.SetOptions;

import org.apache.commons.lang3.SerializationUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import kotlin.Unit;

import static android.app.Activity.RESULT_OK;
import static com.everlog.constants.ELConstants.EXTRA_EXERCISE_GROUPS;

public class PresenterWorkoutDetails extends BaseActivityPresenter<MvpViewWorkoutDetails> {

    // From the workout loading. Lets the congrats banner, shown about 0.5s in, play before Play's rating
    // dialog covers the screen.
    private static final long RATE_PROMPT_DELAY_MILLIS = 3500;

    private ELWorkout toEdit;
    private boolean mLoadedItemOnce;
    private boolean mBackPressed;
    private boolean mChangesMade;
    private final RecyclerAdapter mAdapter = new RecyclerAdapter();
    private final DataListManager<Object> mDataListManager = new DataListManager<>(mAdapter);

    @Override
    public void init() {
        super.init();
        setupListView();
    }

    @Override
    public void onReady() {
        setupEditedItem();
        observeSaveAsRoutineClick();
        observeDeleteClick();
        observeShareClick();
        loadWorkout();
    }

    @Override
    public void detachView() {
        ELDatastore.workoutStore().destroy();
        super.detachView();
    }

    @Override
    public boolean onBackPressedConsumed() {
        mBackPressed = true;
        if (getMvpView().isJustFinished() && !toEdit.getFromRoutine()) {
            // Check if user wants to save session to their workouts
            observeSaveAsRoutineConfirm();
        } else {
            saveChangesAndCloseScreen();
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_EXERCISE_GROUPS) {
            if (resultCode == RESULT_OK
                    && data != null
                    && data.hasExtra(EXTRA_EXERCISE_GROUPS)) {
                handleEditExercises((List<ELExerciseGroup>) data.getSerializableExtra(EXTRA_EXERCISE_GROUPS));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutLoaded(ELDocStoreWorkoutLoadedEvent event) {
        if (isAttachedToView()) {
            if (event.getError() != null) {
                handleError(event.getError());
            } else {
                if (!mLoadedItemOnce || event.isHasPendingWrites()) {
                    toEdit = event.getItem();
                    loadViewData();
                    if (getMvpView().isJustFinished() && !mLoadedItemOnce) {
                        Utils.runWithDelay(() -> {
                            handleCheckWeeklyGoalComplete();
                        }, 500);
                        PresenterNavigator.getNavigator(this).requestRatePrompt(new RatePromptTrigger.WorkoutCompleted(toEdit), RATE_PROMPT_DELAY_MILLIS);
                    }
                }
                mLoadedItemOnce = true;
            }
        }
    }

    RecyclerAdapter getListAdapter() {
        return mAdapter;
    }

    // Observers

    private void observeShareClick() {
        subscriptions.add(getMvpView().onClickShare()
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    AnalyticsManager.manager.workoutDetailsShared();
                    PresenterNavigator.getNavigator(this).shareWorkout(toEdit);
                }, throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineClick() {
        subscriptions.add(getMvpView().onClickSaveAsRoutine()
                .compose(applyUISchedulers())
                .subscribe(action -> handleSaveAsRoutine(), throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineConfirm() {
        subscriptions.add(getMvpView().showPrompt(R.string.workout_details_save_title, R.string.workout_details_save_prompt, R.string.workout_details_save_title, R.string.no_thanks)
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleSaveAsRoutine();
                    } else {
                        // This happens only after pressing BACK so safely close the screen.
                        saveChangesAndCloseScreen();
                    }
                }, throwable -> handleError(throwable)));
    }

    private void observeDeleteClick() {
        subscriptions.add(getMvpView().onClickDelete()
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    observeDeleteConfirm(toEdit);
                }, throwable -> handleError(throwable)));
    }

    private void observeDeleteConfirm(ELWorkout workout) {
        subscriptions.add(getMvpView().showPrompt(com.everlog.android.common.design.R.string.delete_title, com.everlog.android.common.design.R.string.delete_prompt, com.everlog.android.common.design.R.string.delete, com.everlog.android.common.design.R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        AnalyticsManager.manager.workoutDetailsDeleted();
                        deleteWorkout(workout);
                    }
                }, throwable -> handleError(throwable)));
    }

    private void observeEditNameConfirm() {
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getName(), AppDialogs.StringDialogType.WORKOUT_NAME)
                .compose(applyUISchedulers())
                .subscribe(newName -> {
                    toEdit.setName(newName);
                    mAdapter.notifyDataSetChanged();
                    changesMade();
                }));
    }

    private void observeEditDateConfirm() {
        subscriptions.add(getMvpView().showDatePrompt(toEdit.getCompletedDateAsDate())
                .onBackpressureBuffer()
                .compose(applyUISchedulers())
                .subscribe(date -> {
                    if (!DateExtKt.isFuture(date)) {
                        toEdit.setCompletedDateAsDate(date);
                        mAdapter.notifyDataSetChanged();
                        changesMade();
                    } else {
                        getMvpView().showToast(R.string.workout_details_date_future_error);
                    }
                }));
    }

    private void observeEditNoteConfirm() {
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getNote(), AppDialogs.StringDialogType.WORKOUT_NOTE)
                .compose(applyUISchedulers())
                .subscribe(note -> {
                    toEdit.setNote(TextUtils.isEmpty(note) ? null : note);
                    mAdapter.notifyDataSetChanged();
                    changesMade();
                }));
    }

    // Loading

    private void loadWorkout() {
        loadViewData();
        // Listen for item changes
        ELDatastore.workoutStore().getItem(toEdit.getUuid());
    }

    private void loadViewData() {
        // Show routine immediately
        getMvpView().loadItemDetails(toEdit);
        mDataListManager.clear();
        mDataListManager.add(toEdit);
        mDataListManager.addAll(toEdit.getExerciseGroups());
        mAdapter.notifyDataSetChanged();
        handleCheckEmptyState();
    }

    // Saving

    private void deleteWorkout(ELWorkout workout) {
        ELDatastore.workoutStore().delete(workout);
        getMvpView().closeScreen();
    }

    private void saveChangesAndCloseScreen() {
        if (TextUtils.isEmpty(toEdit.getName())) {
            getMvpView().showToast(R.string.create_routine_error_no_title);
        } else {
            if (mChangesMade) {
                ELDatastore.workoutStore().create(toEdit, SetOptions.merge());
                getMvpView().showToast(R.string.create_routine_saved);
            }
        }
        getMvpView().closeScreen();
    }

    private void changesMade() {
        mChangesMade = true;
        getMvpView().showSaveChangesTick();
    }

    // Handlers

    private void handleSaveAsRoutine() {
        AnalyticsManager.manager.workoutDetailsSavedAsRoutine();
        // Save routine
        ELRoutine toSave = SerializationUtils.clone(toEdit).getRoutineFromWorkout();
        ELDatastore.routineStore().create(toSave, SetOptions.merge());
        AnalyticsManager.manager.routineCreated();
        // Replace routine for ongoing plan
        PlanManager.manager.updateRoutineForPlan(getMvpView().getContext(), toSave);
        getMvpView().showToast(R.string.workout_details_save_success);
        if (mBackPressed) {
            // If we are coming from the BACK workflow, safely close the screen.
            saveChangesAndCloseScreen();
        }
    }

    private void handleCheckEmptyState() {
        getMvpView().toggleEmptyViewVisible(mDataListManager.isEmpty());
    }

    private void handleCheckWeeklyGoalComplete() {
        // Fetch all workouts.
        ELDatastore.workoutsStore().getItems(new OnStoreItemsListener<ELWorkout>() {

            @Override
            public void onItemsLoaded(List<ELWorkout> items, boolean fromCache) {
                // Calculate weekly stats.
                new UserStatsController().loadStats(RangeType.WEEK,
                        items,
                        new UserStatsController.OnCompleteListener() {

                    @Override
                    public void onComplete(@NotNull UserStatsController.StatsResult result) {
                        if (isAttachedToView()) {
                            if (getMvpView().performingFromPlan()) {
                                getMvpView().showWorkoutCompleteCongrats();
                            } else if (SettingsManager.manager.weeklyWorkoutsGoal() == result.getWorkoutsCompleted()) {
                                getMvpView().showWeeklyGoalCompleteCongrats();
                            } else {
                                getMvpView().showWorkoutCompleteCongrats();
                            }
                        }
                    }

                    @Override
                    public void onError(@NotNull Throwable throwable) {
                        handleError(throwable);
                    }
                });
            }

            @Override
            public void onItemsLoadingError(Throwable throwable) {
                handleError(throwable);
            }
        });
    }

    private void handleEditExercises(List<ELExerciseGroup> groups) {
        toEdit.setExerciseGroups(groups);
        loadViewData();
        changesMade();
    }

    // Setup

    private void setupListView() {
        mAdapter.addDataManager(mDataListManager);
        mAdapter.registerBinders(new WorkoutDetailsStatsAdapter.Binder(new WorkoutDetailsStatsAdapter.OnWorkoutDetailsStatsListener() {
            @Override
            public void onClickEditName(ELWorkout workout) {
                AnalyticsManager.manager.workoutDetailsNameModified();
                observeEditNameConfirm();
            }

            @Override
            public void onClickEditDate(ELWorkout workout) {
                AnalyticsManager.manager.workoutDetailsDateModified();
                observeEditDateConfirm();
            }

            @Override
            public void onClickEditNote(ELWorkout workout) {
                AnalyticsManager.manager.workoutDetailsNotesModified();
                observeEditNoteConfirm();
            }

            @Override
            public void onClickEditExercises(ELWorkout workout) {
                AnalyticsManager.manager.workoutDetailsExercisesModified();
                PresenterNavigator.getNavigator(PresenterWorkoutDetails.this).openEditExerciseGroups(new DefaultCreateExerciseGroupsActivity.Companion.Properties()
                        .groups(workout.getExerciseGroups()));
            }

            @Override
            public void onItemClicked(ELWorkout item, int position) {}
        }), new ExerciseGroupSummaryAdapter().build(new ExerciseGroupSummaryAdapter.Builder()
                .setShowTemplates(false)
                .setWeightUnit(SettingsManager.manager.weightUnit())
                .setOnExerciseClicked((context, exercise) -> {
                    new ELNavigator(context).openExerciseDetails(new ExerciseDetailsActivity.Companion.Properties().exercise(exercise));
                    return Unit.INSTANCE;
                })
        ));
    }

    private void setupEditedItem() {
        toEdit = getMvpView().getItemToEdit();
    }
}