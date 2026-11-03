package com.everlog.android.workout.details;

import com.everlog.android.template.data.session.OngoingPlanRepository;
import com.everlog.android.workout.WorkoutStats;
import com.everlog.android.workout.WorkoutNavigation;
import com.everlog.android.settings.data.UserSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;

import com.ahamed.multiviewadapter.DataListManager;
import com.ahamed.multiviewadapter.RecyclerAdapter;
import com.everlog.android.common.kotlin.DateExtKt;
import com.everlog.android.template.data.store.TemplateStores;
import com.everlog.android.workout.R;
import com.everlog.android.common.data.store.OnStoreItemsListener;
import com.everlog.android.workout.data.store.ELDocStoreWorkoutLoadedEvent;
import com.everlog.android.workout.data.store.WorkoutStores;
import com.everlog.android.common.data.model.ELRoutine;
import com.everlog.android.common.data.model.exercise.ELExerciseGroup;
import com.everlog.android.common.data.model.workout.ELWorkout;
import com.everlog.android.common.mvp.activity.BaseActivityPresenter;
import com.everlog.android.common.kotlin.Utils;
import com.everlog.android.common.mvp.list.ExerciseGroupSummaryAdapter;
import com.everlog.android.common.navigation.ExerciseTab;
import com.google.firebase.firestore.SetOptions;

import org.apache.commons.lang3.SerializationUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import kotlin.Unit;

public class PresenterWorkoutDetails extends BaseActivityPresenter<MvpViewWorkoutDetails> {

    private final UserSettings settings;
    private final WorkoutDetailsAnalytics analytics;
    private final WorkoutNavigation navigation;
    private final WorkoutStats stats;
    private final OngoingPlanRepository ongoingPlan;

    // From the workout loading. Lets the congrats banner, shown about 0.5s in, play before Play's rating
    // dialog covers the screen.
    private static final long RATE_PROMPT_DELAY_MILLIS = 3500;

    private ELWorkout toEdit;
    private boolean mLoadedItemOnce;

    public PresenterWorkoutDetails(UserSettings settings, WorkoutDetailsAnalytics analytics, WorkoutNavigation navigation, WorkoutStats stats, OngoingPlanRepository ongoingPlan) {
        this.settings = settings;
        this.analytics = analytics;
        this.navigation = navigation;
        this.stats = stats;
        this.ongoingPlan = ongoingPlan;
    }
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
        WorkoutStores.workoutStore().destroy();
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
        List<ELExerciseGroup> edited = navigation.editedExerciseGroups(requestCode, resultCode, data);
        if (edited != null) {
            handleEditExercises(edited);
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
                        navigation.requestRatePrompt(getMvpView().getActivity(), toEdit, RATE_PROMPT_DELAY_MILLIS);
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
                    analytics.shared();
                    navigation.shareWorkout(getMvpView().getActivity(), toEdit);
                }, throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineClick() {
        subscriptions.add(getMvpView().onClickSaveAsRoutine()
                .compose(applyUISchedulers())
                .subscribe(action -> handleSaveAsRoutine(), throwable -> handleError(throwable)));
    }

    private void observeSaveAsRoutineConfirm() {
        subscriptions.add(getMvpView().showPrompt(R.string.workout_details_save_title, R.string.workout_details_save_prompt, R.string.workout_details_save_title, com.everlog.android.common.design.R.string.no_thanks)
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
                        analytics.deleted();
                        deleteWorkout(workout);
                    }
                }, throwable -> handleError(throwable)));
    }

    private void observeEditNameConfirm() {
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getName(), WorkoutTextField.Name)
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
        subscriptions.add(getMvpView().showStringPrompt(toEdit.getNote(), WorkoutTextField.Note)
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
        WorkoutStores.workoutStore().getItem(toEdit.getUuid());
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
        WorkoutStores.workoutStore().delete(workout);
        getMvpView().closeScreen();
    }

    private void saveChangesAndCloseScreen() {
        if (TextUtils.isEmpty(toEdit.getName())) {
            getMvpView().showToast(com.everlog.android.common.design.R.string.create_routine_error_no_title);
        } else {
            if (mChangesMade) {
                WorkoutStores.workoutStore().create(toEdit, SetOptions.merge());
                getMvpView().showToast(com.everlog.android.common.design.R.string.create_routine_saved);
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
        analytics.savedAsTemplate();
        // Save routine
        ELRoutine toSave = SerializationUtils.clone(toEdit).getRoutineFromWorkout();
        TemplateStores.routineStore().create(toSave, SetOptions.merge());
        // Replace routine for ongoing plan
        ongoingPlan.replaceRoutine(toSave);
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
        WorkoutStores.workoutsStore().getItems(new OnStoreItemsListener<ELWorkout>() {

            @Override
            public void onItemsLoaded(List<ELWorkout> items, boolean fromCache) {
                stats.workoutsThisWeek(items, workoutsThisWeek -> {
                        if (isAttachedToView()) {
                            if (getMvpView().performingFromPlan()) {
                                getMvpView().showWorkoutCompleteCongrats();
                            } else if (settings.weeklyWorkoutsGoal() == workoutsThisWeek) {
                                getMvpView().showWeeklyGoalCompleteCongrats();
                            } else {
                                getMvpView().showWorkoutCompleteCongrats();
                            }
                        }
                    return Unit.INSTANCE;
                }, throwable -> {
                    handleError(throwable);
                    return Unit.INSTANCE;
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
        mAdapter.registerBinders(new WorkoutDetailsStatsAdapter.Binder(settings.weightUnit(), new WorkoutDetailsStatsAdapter.OnWorkoutDetailsStatsListener() {
            @Override
            public void onClickEditName(ELWorkout workout) {
                analytics.nameModified();
                observeEditNameConfirm();
            }

            @Override
            public void onClickEditDate(ELWorkout workout) {
                analytics.dateModified();
                observeEditDateConfirm();
            }

            @Override
            public void onClickEditNote(ELWorkout workout) {
                analytics.notesModified();
                observeEditNoteConfirm();
            }

            @Override
            public void onClickEditExercises(ELWorkout workout) {
                analytics.exercisesModified();
                navigation.openExerciseEditor(getMvpView().getActivity(), workout.getExerciseGroups());
            }

            @Override
            public void onItemClicked(ELWorkout item, int position) {}
        }), new ExerciseGroupSummaryAdapter().build(new ExerciseGroupSummaryAdapter.Builder()
                .setShowTemplates(false)
                .setWeightUnit(settings.weightUnit())
                .setOnExerciseClicked((context, exercise) -> {
                    navigation.openExerciseDetails(context, exercise, ExerciseTab.Info);
                    return Unit.INSTANCE;
                })
        ));
    }

    private void setupEditedItem() {
        toEdit = getMvpView().getItemToEdit();
    }
}