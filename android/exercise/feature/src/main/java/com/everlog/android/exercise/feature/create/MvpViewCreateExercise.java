package com.everlog.android.exercise.feature.create;

import com.everlog.android.common.data.model.exercise.ELExercise;
import com.everlog.android.common.mvp.activity.BaseActivityMvpView;
import com.imagepick.ImagePickerOptions;
import com.imagepick.ImagePickerResult;

import rx.Observable;

public interface MvpViewCreateExercise extends BaseActivityMvpView {

    Observable<Void> onClickSave();

    Observable<Integer> onCategoryChanged();

    Observable<CharSequence> onNameChanged();

    Observable<Void> onClickImage();

    ELExercise getItemToEdit();

    // For a new exercise, the name it starts with, if any
    String getNameToCreate();

    String getExerciseName();

    int getSelectedCategoryIndex();

    void loadExerciseDetails(ELExercise exercise, int selectedCategoryIndex);

    void setExerciseImage(ELExercise exercise);

    Observable<ImagePickerResult> showImagePicker(ImagePickerOptions options);
}
