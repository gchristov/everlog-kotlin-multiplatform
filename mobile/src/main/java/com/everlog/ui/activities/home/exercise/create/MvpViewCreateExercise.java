package com.everlog.ui.activities.home.exercise.create;

import com.everlog.data.model.exercise.ELExercise;
import com.everlog.ui.activities.base.BaseActivityMvpView;
import com.imagepick.picker.NewImagePickerOptions;
import com.imagepick.picker.NewImagePickerResult;

import rx.Observable;

public interface MvpViewCreateExercise extends BaseActivityMvpView {

    Observable<Void> onClickSave();

    Observable<Integer> onCategoryChanged();

    Observable<CharSequence> onNameChanged();

    Observable<Void> onClickImage();

    ELExercise getItemToEdit();

    String getExerciseName();

    int getSelectedCategoryIndex();

    void loadExerciseDetails(ELExercise exercise, int selectedCategoryIndex);

    void setExerciseImage(ELExercise exercise);

    Observable<NewImagePickerResult> showImagePicker(NewImagePickerOptions options);
}
