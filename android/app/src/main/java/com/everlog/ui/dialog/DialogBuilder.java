package com.everlog.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;

import com.everlog.R;

import java.util.Date;

import androidx.annotation.Nullable;
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
        return new Pair<>(positiveButtonPublish, dialog);
    }

    /**
     * A prompt confirming a destructive action, e.g. Delete or Discard. As in the workout's discard prompt,
     * cancelling is the primary button and the action the secondary one. Emits as {@link #showPrompt} does:
     * {@link DialogInterface#BUTTON_POSITIVE} for the action and {@link DialogInterface#BUTTON_NEGATIVE} for
     * cancel, whichever button shows each.
     */
    public static Observable<Integer> showDestructivePrompt(Context context,
                                                            String title,
                                                            String message,
                                                            String action,
                                                            String cancel) {
        PublishSubject<Integer> buttonPublish = PublishSubject.create();

        // Cancel is on the positive (primary) button and the action on the negative (secondary) one
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            buttonPublish.onNext(which == DialogInterface.BUTTON_NEGATIVE
                    ? DialogInterface.BUTTON_POSITIVE
                    : DialogInterface.BUTTON_NEGATIVE);
            dialog.dismiss();
        };

        new AlertDialog.Builder(context, R.style.DarkDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(cancel, dialogClickListener)
                .setNegativeButton(action, dialogClickListener)
                .create()
                .show();
        return buttonPublish;
    }

    /**
     * Emitted by {@link #showChoicePrompt} when the dialog is closed without tapping a button, i.e. with back.
     */
    public static final int PROMPT_DISMISSED = 0;

    /**
     * A prompt with up to three buttons. Emits the tapped button (e.g. {@link DialogInterface#BUTTON_NEUTRAL}),
     * or {@link #PROMPT_DISMISSED}. As in the Compose AppDialog, positive is the primary action, negative the
     * secondary one and neutral any other (see DarkDialogTheme).
     *
     * @param neutral the third button, or null for just two
     */
    public static Observable<Integer> showChoicePrompt(Context context,
                                                       String title,
                                                       String message,
                                                       String positive,
                                                       String negative,
                                                       @Nullable String neutral) {
        PublishSubject<Integer> buttonPublish = PublishSubject.create();

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            buttonPublish.onNext(which);
            dialog.dismiss();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DarkDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, dialogClickListener)
                .setNegativeButton(negative, dialogClickListener)
                .setOnCancelListener(dialog -> buttonPublish.onNext(PROMPT_DISMISSED));
        if (neutral != null) {
            builder.setNeutralButton(neutral, dialogClickListener);
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        return buttonPublish;
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
