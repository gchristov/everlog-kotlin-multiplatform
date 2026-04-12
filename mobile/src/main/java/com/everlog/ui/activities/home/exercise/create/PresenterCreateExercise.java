package com.everlog.ui.activities.home.exercise.create;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.everlog.R;
import com.everlog.data.datastores.ELDatastore;
import com.everlog.data.migration.Migration;
import com.everlog.data.migration.MultiPartUpdateExercise;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.firebase.FirebaseStorageManager;
import com.everlog.managers.firebase.FirestorePathManager;
import com.everlog.ui.activities.base.BaseActivityPresenter;
import com.everlog.utils.ArrayResourceTypeUtils;
import com.everlog.utils.Utils;
import com.everlog.utils.input.KeyboardUtils;
import com.google.firebase.firestore.SetOptions;
import com.imagepick.client.ELImagePicker;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

//import icepick.State;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class PresenterCreateExercise extends BaseActivityPresenter<MvpViewCreateExercise> {

//    @State
    boolean mChangesMade = false;
    private ELExercise toEdit;
    private boolean mInitialDataSet = false;
    private boolean mEditMode = false;
    private int mImagePickRequestCode;
    private Uri mPickedImageUri;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == mImagePickRequestCode) {
            if (resultCode == RESULT_OK
                    && data != null
                    && data.hasExtra("path")) {
                Uri uri = Objects.requireNonNull(data).getParcelableExtra("path");
                handleImagePicked(uri);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        subscriptions.add(getMvpView().showPrompt(R.string.discard_title, R.string.discard_prompt, R.string.discard, R.string.cancel)
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
                ELDatastore.exerciseStore().create(toSave, SetOptions.merge());
                getMvpView().showToast(R.string.create_exercise_saved);
                if (mEditMode) {
                    AnalyticsManager.manager.exerciseModified();
                } else {
                    AnalyticsManager.manager.exerciseCreated();
                }
                // Upload image.
                if (mPickedImageUri != null) {
                    FirebaseStorageManager.uploadExerciseImage(mPickedImageUri, toSave);
                }
                if (toSave.getImageUrl() == null) {
                    FirebaseStorageManager.deleteExerciseImage(toSave);
                }
                // Update routines and history.
                handleMigrateRoutinesAndHistory(toSave);
            }
            getMvpView().setViewResult(RESULT_OK);
            getMvpView().closeScreen();
        }
    }

    private void handleMigrateRoutinesAndHistory(ELExercise changed) {
        Migration migration = new MultiPartUpdateExercise(changed);
        migration.migrate(FirestorePathManager.INSTANCE);
    }

    private void handlePickImage() {
        mImagePickRequestCode = ELImagePicker
                .withActivity(getMvpView().getActivity())
                .withPermissionErrorListener(() -> {
                    navigator.promptForAppSettings(R.string.create_exercise_error_no_permissions);
                })
                .withImageRemoveListener(toEdit.getImageUrl() == null ? null : () -> {
                    handleImagePicked(null);
                })
                .pick();
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
            toEdit = ELExercise.newExercise(getUserAccount().getId());
        }
    }
}
