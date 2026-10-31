package com.everlog.android.workout.details;

import com.everlog.android.common.data.model.workout.ELWorkout;
import com.everlog.android.common.mvp.activity.BaseActivityMvpView;

import java.util.Date;

import rx.Observable;

public interface MvpViewWorkoutDetails extends BaseActivityMvpView {

    Observable<Void> onClickSaveAsRoutine();

    Observable<Void> onClickDelete();

    Observable<Void> onClickShare();

    ELWorkout getItemToEdit();

    Observable<String> showStringPrompt(String name, WorkoutTextField type);

    Observable<Date> showDatePrompt(Date date);

    boolean isJustFinished();

    boolean performingFromPlan();

    void loadItemDetails(ELWorkout workout);

    void toggleEmptyViewVisible(boolean visible);

    void showWeeklyGoalCompleteCongrats();

    void showWorkoutCompleteCongrats();

    void showSaveChangesTick();
}
