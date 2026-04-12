package com.everlog.ui.activities.home.workout.details;

import com.everlog.data.model.workout.ELWorkout;
import com.everlog.ui.activities.base.BaseActivityMvpView;
import com.everlog.ui.dialog.DialogBuilder;

import java.util.Date;

import rx.Observable;

public interface MvpViewWorkoutDetails extends BaseActivityMvpView {

    Observable<Void> onClickSaveAsRoutine();

    Observable<Void> onClickDelete();

    Observable<Void> onClickShare();

    ELWorkout getItemToEdit();

    Observable<String> showStringPrompt(String name, DialogBuilder.StringDialogType type);

    Observable<Date> showDatePrompt(Date date);

    boolean isJustFinished();

    boolean performingFromPlan();

    void loadItemDetails(ELWorkout workout);

    void toggleEmptyViewVisible(boolean visible);

    void showWeeklyGoalCompleteCongrats();

    void showWorkoutCompleteCongrats();

    void showSaveChangesTick();
}
