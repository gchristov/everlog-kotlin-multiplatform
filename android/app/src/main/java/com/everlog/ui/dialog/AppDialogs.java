package com.everlog.ui.dialog;

import android.content.Context;

import java.util.Date;

import rx.Observable;

public class AppDialogs {

    public enum MetricDialogType {
        WEIGHT,
        REPS,
        REPS_REQUIRED
    }

    public enum NumberPickerDialogType {
        WEEKLY_GOAL
    }

    public enum DurationPickerDialogType {
        REST_TIME,
        REST_TIME_REQUIRED,
        EXERCISE_TIME,
        EXERCISE_TIME_REQUIRED
    }


    public enum StringDialogType {
        ROUTINE_NAME,
        WORKOUT_NAME,
        WORKOUT_NOTE
    }

    public enum AppBlockerDialogType {
        NEWSLETTER,
    }

	public static Observable<String> showInputStringDialog(Context context,
                                                           String currentValue,
                                                           StringDialogType type) {
        return InputDialogs.showInputStringDialog(context, currentValue, type);
	}

    public static Observable<String> showInputNumberDialog(Context context,
                                                           String value,
                                                           MetricDialogType type) {
        return InputDialogs.showInputNumberDialog(context, value, type);
    }

    public static Observable<Float> showWeightIncreaseDialog(Context context, float value) {
        return NumberDialogs.showWeightIncreaseDialog(context, value);
    }

    public static Observable<Integer> showPickerNumberDialog(Context context,
                                                             int value,
                                                             NumberPickerDialogType type) {
        return NumberDialogs.showPickerNumberDialog(context, value, type);
    }

    public static Observable<Integer> showPickerDurationDialog(Context context,
                                                               int valueSeconds,
                                                               DurationPickerDialogType type) {
        return NumberDialogs.showPickerDurationDialog(context, valueSeconds, type);
    }


    public static Observable<Date> showDateTimeDialog(Context context, Date date) {
        return DateTimeDialogs.showDateDialog(context, date);
    }

    public static Observable<Integer> showAppBlockerDialog(Context context, AppBlockerDialogType type) {
        return AppBlockerDialogs.showAppBlockerDialog(context, type);
    }
}
