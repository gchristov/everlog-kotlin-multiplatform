package com.everlog.android.exercise.create;

import com.everlog.android.common.navigation.AppSettingsPrompt;
import com.everlog.android.common.navigation.Navigator;
import com.everlog.android.exercise.ExerciseAnalytics;
import android.content.Intent;
import android.content.DialogInterface;
import android.net.Uri;

import com.everlog.android.common.data.model.exercise.ELExercise;
import com.everlog.android.auth.data.UserSession;
import com.everlog.android.auth.data.UserSessionKt;
import com.everlog.android.exercise.data.ExerciseImageStorage;
import com.everlog.android.exercise.data.store.ExerciseStores;
import com.everlog.android.exercise.ExerciseExtras;
import com.everlog.android.exercise.R;
import com.everlog.android.common.mvp.activity.BaseActivityPresenter;
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils;
import com.everlog.android.common.kotlin.Utils;
import com.everlog.android.common.mvp.util.KeyboardUtils;
import com.everlog.android.exercise.data.ExerciseCopies;
import com.google.firebase.firestore.SetOptions;
import com.imagepick.ImagePickerOptions;
import com.imagepick.ImagePickerResult;

//import icepick.State;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class PresenterCreateExercise extends BaseActivityPresenter<MvpViewCreateExercise> {

//    @State
    boolean mChangesMade = false;
    private final UserSession session;
    private final ExerciseImageStorage imageStorage;
    private final ExerciseCopies copies;
    private final ExerciseAnalytics analytics;
    private final Navigator navigator;
    private ELExercise toEdit;
    private boolean mInitialDataSet = false;
    private boolean mEditMode = false;
    private Uri mPickedImageUri;

    public PresenterCreateExercise(UserSession session, ExerciseImageStorage imageStorage, ExerciseCopies copies, ExerciseAnalytics analytics, Navigator navigator) {
        this.session = session;
        this.imageStorage = imageStorage;
        this.copies = copies;
        this.analytics = analytics;
        this.navigator = navigator;
    }

    @Override
    public void onReady() {
        setupEditedItem();
        observeDynamicChanges();
        observeSaveClick();
        observeImageClick();
        loadViewData();
    }

    @Override
    public boolean onBackPressedConsumed() {
        if (mChangesMade) {
            observeDiscard();
            return true;
        } else {
            return super.onBackPressedConsumed();
        }
    }

    // Observers

    private void observeImageClick() {
        subscriptions.add(getMvpView().onClickImage()
                .compose(applyUISchedulers())
                .subscribe(action -> handlePickImage(), throwable -> handleError(throwable)));
    }

    private void observeDynamicChanges() {
        subscriptions.add(getMvpView().onCategoryChanged()
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    handleChangesMade();
                    KeyboardUtils.hideKeyboard(getMvpView().getActivity());
                }, throwable -> handleError(throwable)));
        subscriptions.add(getMvpView().onNameChanged()
                .compose(applyUISchedulers())
                .subscribe(action -> handleChangesMade(), throwable -> handleError(throwable)));
    }

    private void observeDiscard() {
        subscriptions.add(getMvpView().showDestructivePrompt(com.everlog.android.common.design.R.string.discard_title, com.everlog.android.common.design.R.string.discard_prompt, com.everlog.android.common.design.R.string.discard, com.everlog.android.common.design.R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        getMvpView().setViewResult(RESULT_CANCELED);
                        getMvpView().closeScreen();
                    }
                }, throwable -> handleError(throwable)));
    }

    private void observeSaveClick() {
        subscriptions.add(getMvpView().onClickSave()
                .compose(applyUISchedulers())
                .subscribe(action -> handleSaveExercise(), throwable -> handleError(throwable)));
    }

    // Loading

    private void loadViewData() {
        int categoryIndex = toEdit.getCategory() == null ? -1 : ArrayResourceTypeUtils.withExerciseCategories().getTypeIndex(toEdit.getCategory());
        getMvpView().loadExerciseDetails(toEdit, categoryIndex);
        Utils.runWithDelay(() -> {
            mInitialDataSet = true;
        }, 300);
    }

    // Handlers

    private void handleChangesMade() {
        if (mInitialDataSet) {
            mChangesMade = true;
        }
    }

    private void handleSaveExercise() {
        KeyboardUtils.hideKeyboard(getMvpView().getActivity());
        String name = getMvpView().getExerciseName();
        int categoryIndex = getMvpView().getSelectedCategoryIndex();
        if (name.isEmpty()) {
            getMvpView().showToast(R.string.create_exercise_error_no_title);
        } else if (categoryIndex < 0) {
            getMvpView().showToast(R.string.create_exercise_error_no_category);
        } else {
            if (mChangesMade) {
                // Save exercise.
                ELExercise toSave = buildChangedItem();
                ExerciseStores.exerciseStore().create(toSave, SetOptions.merge());
                getMvpView().showToast(R.string.create_exercise_saved);
                if (mEditMode) {
                    analytics.exerciseModified();
                } else {
                    analytics.exerciseCreated();
                }
                // Upload image.
                if (mPickedImageUri != null) {
                    imageStorage.uploadExerciseImage(mPickedImageUri, toSave, UserSessionKt.requireUserId(session));
                }
                if (toSave.getImageUrl() == null) {
                    imageStorage.deleteExerciseImage(toSave, UserSessionKt.requireUserId(session));
                }
                // Update routines and history.
                handleMigrateRoutinesAndHistory(toSave);
            }
            // The exercise's uuid, so a picker can pick the one just created
            getMvpView().setViewResult(RESULT_OK, new Intent().putExtra(ExerciseExtras.EXERCISE_UUID, toEdit.getUuid()));
            getMvpView().closeScreen();
        }
    }

    private void handleMigrateRoutinesAndHistory(ELExercise changed) {
        copies.update(changed);
    }

    private void handlePickImage() {
        ImagePickerOptions options = new ImagePickerOptions(
                com.everlog.android.common.mvp.R.string.select_image,
                toEdit.getImageUrl() != null,
                1, 1,
                512, 512,
                true
        );
        subscriptions.add(getMvpView().showImagePicker(options)
                .compose(applyUISchedulers())
                .subscribe(result -> {
                    if (result instanceof ImagePickerResult.Success) {
                        handleImagePicked(((ImagePickerResult.Success) result).getUri());
                    } else if (result instanceof ImagePickerResult.Removed) {
                        handleImagePicked(null);
                    } else if (result instanceof ImagePickerResult.PermissionDenied) {
                        navigator.open(getContext(), new AppSettingsPrompt(R.string.create_exercise_error_no_permissions));
                    }
                }, this::handleError));
    }

    private void handleImagePicked(Uri uri) {
        if (isAttachedToView()) {
            mPickedImageUri = uri;
            toEdit.setImageUrl(uri == null ? null : uri.toString());
            getMvpView().setExerciseImage(toEdit);
            handleChangesMade();
        }
    }

    // Item changes

    private ELExercise buildChangedItem() {
        toEdit.setName(getMvpView().getExerciseName());
        toEdit.setSplitSides(false);
        toEdit.setCategory(ArrayResourceTypeUtils.withExerciseCategories().getType(getMvpView().getSelectedCategoryIndex()));
        return toEdit;
    }

    // Setup

    private void setupEditedItem() {
        toEdit = getMvpView().getItemToEdit();
        mEditMode = toEdit != null;
        if (toEdit == null) {
            toEdit = ELExercise.newExercise(UserSessionKt.requireUserId(session));
            toEdit.setName(getMvpView().getNameToCreate());
        }
    }
}
