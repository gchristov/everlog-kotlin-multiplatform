package com.everlog.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;
import android.widget.Button;

import com.everlog.R;

import java.util.Date;

import androidx.core.content.ContextCompat;
import rx.Observable;
import rx.subjects.PublishSubject;

public class DialogBuilder {

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

    public enum MultipleChoiceDialogType {
        UNIT_WEIGHT,
        FIRST_DAY_OF_WEEK,
        EXERCISE_CATEGORY
    }

    public enum StringDialogType {
        ROUTINE_NAME,
        WORKOUT_NAME,
        WORKOUT_NOTE
    }

    public enum AppBlockerDialogType {
        NEWSLETTER,
    }

    public static Observable<Void> showOKPrompt(Context context, String title, String message) {
        PublishSubject<Void> okPublish = PublishSubject.create();

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    okPublish.onNext(null);
                    break;
            }
            dialog.dismiss();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DarkDialogTheme);
        AlertDialog dialog = builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, dialogClickListener)
                .create();
        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent));
        });
        dialog.show();
        return okPublish;
    }

    public static Observable<Integer> showPrompt(Context context, String title, String message, String yes, String no) {
		Pair<Observable<Integer>, AlertDialog> data = buildPrompt(context, title, message, yes, no);
        data.second.show();
		return data.first;
	}

	public static Pair<Observable<Integer>, AlertDialog> buildPrompt(Context context,
                                                                     String title,
                                                                     String message,
                                                                     String yes,
                                                                     String no) {
        PublishSubject<Integer> positiveButtonPublish = PublishSubject.create();

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    positiveButtonPublish.onNext(DialogInterface.BUTTON_POSITIVE);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    positiveButtonPublish.onNext(DialogInterface.BUTTON_NEGATIVE);
                    break;
            }
            dialog.dismiss();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DarkDialogTheme);
        AlertDialog dialog = builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(yes, dialogClickListener)
                .setNegativeButton(no, dialogClickListener)
                .create();
        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent));
            Button negativeBtn = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent));
        });
        return new Pair<>(positiveButtonPublish, dialog);
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

    public static Observable<Integer> showPickerMultipleChoiceDialog(Context context,
                                                                     String[] options,
                                                                     int selectedIndex,
                                                                     MultipleChoiceDialogType type) {
        return MultipleChoiceDialogs.showPickerMultipleChoiceDialog(context, options, selectedIndex, type);
    }

    public static Observable<Date> showDateTimeDialog(Context context, Date date) {
        return DateTimeDialogs.showDateDialog(context, date);
    }

    public static Observable<Integer> showAppBlockerDialog(Context context, AppBlockerDialogType type) {
        return AppBlockerDialogs.showAppBlockerDialog(context, type);
    }
}
