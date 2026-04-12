package com.everlog.ui.activities.home.workout.details;

import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;

import com.ahamed.multiviewadapter.DataListManager;
import com.ahamed.multiviewadapter.RecyclerAdapter;
import com.everlog.R;
import com.everlog.constants.ELActivityRequestCodes;
import com.everlog.data.controllers.statistics.UserStatsController;
import com.everlog.data.datastores.ELDatastore;
import com.everlog.data.datastores.base.OnStoreItemsListener;
import com.everlog.data.datastores.history.ELUserWorkoutStore;
import com.everlog.data.model.ELRoutine;
import com.everlog.data.model.exercise.ELExerciseGroup;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.managers.PlanManager;
import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.preferences.SettingsManager;
import com.everlog.ui.activities.base.BaseActivityPresenter;
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity;
import com.everlog.ui.adapters.WorkoutDetailsStatsAdapter;
import com.everlog.ui.adapters.exercise.group.ExerciseGroupSummaryAdapter;
import com.everlog.ui.dialog.DialogBuilder;
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment;
import com.everlog.utils.DateExtKt;
import com.everlog.utils.Utils;
import com.google.firebase.firestore.SetOptions;

import org.apache.commons.lang3.SerializationUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.everlog.constants.ELConstants.EXTRA_EXERCISE_GROUPS;

public class PresenterWorkoutDetails extends BaseActivityPresenter<MvpViewWorkoutDetails> {

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
    public void onWorkoutLoaded(ELUserWorkoutStore.ELDocStoreWorkoutLoadedEvent event) {
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
                    navigator.shareWorkout(toEdit);
                }, throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineClick() {
        subscriptions.add(getMvpView().onClickSaveAsRoutine()
                .compose(applyUISchedulers())
                .subscribe(action -> handleSaveAsRoutine(), throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineConfirm() {
        subscriptions.add(getMvpView().showPrompt(R.string.workout_details_save_title, R.string.workout_details_save_prompt, R.string.workout_details_save_title, R.string.rate_no)
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
        subscriptions.add(getMvpView().showPrompt(R.string.delete_title, R.string.delete_prompt, R.string.delete, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        AnalyticsManager.manager.workoutDetailsDeleted();
                        deleteWorkout(workout);
                    }
                }, throwable -> handleError(throwable)));
    }

    private void observeEditNameConfirm() {
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getName(), DialogBuilder.StringDialogType.WORKOUT_NAME)
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
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getNote(), DialogBuilder.StringDialogType.WORKOUT_NOTE)
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
                new UserStatsController().loadStats(StatisticsHomeFragment.RangeType.WEEK,
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
                navigator.openEditExerciseGroups(new DefaultCreateExerciseGroupsActivity.Companion.Properties()
                        .groups(workout.getExerciseGroups()));
            }

            @Override
            public void onItemClicked(ELWorkout item, int position) {}
        }), new ExerciseGroupSummaryAdapter().build(new ExerciseGroupSummaryAdapter.Builder()
                .setShowTemplates(false)
        ));
    }

    private void setupEditedItem() {
        toEdit = getMvpView().getItemToEdit();
    }
}