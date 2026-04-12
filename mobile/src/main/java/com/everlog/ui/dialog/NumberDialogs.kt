package com.everlog.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.dialog.DialogBuilder.DurationPickerDialogType
import com.everlog.ui.dialog.DialogBuilder.NumberPickerDialogType
import com.everlog.utils.NumberUtils
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import rx.Observable
import rx.subjects.PublishSubject
import java.util.*

internal class NumberDialogs {

    companion object {

        @JvmStatic
        fun showWeightIncreaseDialog(context: Context, value: Float): Observable<Float> {
            val positiveButtonPublish = PublishSubject.create<Float>()
            if (!Utils.isValidContext(context)) {
                return positiveButtonPublish
            }
            val titleResId = R.string.settings_weight_increase
            val iPart = value.toInt()
            val fPart = ((value - iPart) * 100).toInt() // Convert decimal part to number.
            val decimals: List<String> = ArrayList(listOf("0", "25", "50", "75"))
            // Create dialog
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme)
            builder.setTitle(titleResId)
            // Attach buttons
            builder.setPositiveButton(context.getString(R.string.save), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
            // Set custom layout
            val frameView = FrameLayout(context)
            builder.setView(frameView)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val dialogLayout: View = inflater.inflate(R.layout.dialog_picker_weight_increase, frameView)
            val weightUnit = dialogLayout.findViewById<TextView>(R.id.weightUnit)
            weightUnit.text = SettingsManager.weightUnitAbbreviation()
            val wholeNumber: MaterialNumberPicker = dialogLayout.findViewById(R.id.wholeNumberPickerView)
            wholeNumber.minValue = 1
            wholeNumber.maxValue = 20
            wholeNumber.value = iPart
            val decimalNumber: MaterialNumberPicker = dialogLayout.findViewById(R.id.decimalNumberPickerView)
            decimalNumber.minValue = 0
            decimalNumber.maxValue = 3
            decimalNumber.value = decimals.indexOf(fPart.toString() + "")
            // Set visible values for the decimal part
            decimalNumber.displayedValues = decimals.toTypedArray()
            // Styling and listeners
            alertDialog.setOnShowListener { dialog: DialogInterface ->
                val okBlock = Runnable {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    val wholeText = wholeNumber.value
                    val decimalText = decimals[decimalNumber.value].toInt()
                    val result = NumberUtils.parseFloat(String.format("%d.%d", wholeText, decimalText))
                    positiveButtonPublish.onNext(result)
                    dialog.dismiss()
                }
                // Add listeners to dialog buttons
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                positiveBtn.setOnClickListener { okBlock.run() }
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                negativeBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    dialog.dismiss()
                }
            }
            // Show dialog
            alertDialog.show()
            return positiveButtonPublish
        }

        @JvmStatic
        fun showPickerNumberDialog(context: Context,
                                   value: Int,
                                   type: NumberPickerDialogType): Observable<Int> {
            val positiveButtonPublish = PublishSubject.create<Int>()
            if (!Utils.isValidContext(context)) {
                return positiveButtonPublish
            }
            var titleResId = 0
            when (type) {
                NumberPickerDialogType.WEEKLY_GOAL -> titleResId = R.string.settings_weekly_goal
            }
            // Create dialog
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme)
            builder.setTitle(titleResId)
            // Attach buttons
            builder.setPositiveButton(context.getString(R.string.save), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
            // Set custom layout
            val frameView = FrameLayout(context)
            builder.setView(frameView)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val dialogLayout: View = inflater.inflate(R.layout.dialog_picker_number, frameView)
            val inputField: MaterialNumberPicker = dialogLayout.findViewById(R.id.numberPickerView)
            inputField.minValue = 1
            inputField.maxValue = 20
            inputField.value = value
            val unitField = dialogLayout.findViewById<TextView>(R.id.unitField)
            unitField.visibility = View.GONE
            alertDialog.setOnShowListener { dialog: DialogInterface ->
                val okBlock = Runnable {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    val text = inputField.value
                    positiveButtonPublish.onNext(text)
                    dialog.dismiss()
                }
                // Add listeners to dialog buttons
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                positiveBtn.setOnClickListener { okBlock.run() }
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                negativeBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    dialog.dismiss()
                }
            }
            // Show dialog
            alertDialog.show()
            return positiveButtonPublish
        }

        @JvmStatic
        fun showPickerDurationDialog(context: Context,
                                     valueSeconds: Int,
                                     type: DurationPickerDialogType): Observable<Int> {
            val positiveButtonPublish = PublishSubject.create<Int>()
            if (!Utils.isValidContext(context)) {
                return positiveButtonPublish
            }
            val mins = valueSeconds / 60
            val seconds = valueSeconds % 60
            var titleResId = 0
            var clearResId = 0
            var maxAllowedMins = 5
            when (type) {
                DurationPickerDialogType.REST_TIME,
                DurationPickerDialogType.REST_TIME_REQUIRED -> {
                    titleResId = if (type == DurationPickerDialogType.REST_TIME_REQUIRED) R.string.exercise_sets_required_rest_time else R.string.exercise_sets_performed_rest_time
                    clearResId = R.string.disable
                }
                DurationPickerDialogType.EXERCISE_TIME,
                DurationPickerDialogType.EXERCISE_TIME_REQUIRED -> {
                    maxAllowedMins = 60
                    titleResId = if (type == DurationPickerDialogType.EXERCISE_TIME_REQUIRED) R.string.exercise_sets_required_time_prompt else R.string.exercise_sets_performed_time_prompt
                    clearResId = R.string.clear
                }
            }
            // Create dialog
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme)
            builder.setTitle(titleResId)
            // Attach buttons
            builder
                    .setPositiveButton(context.getString(R.string.save), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .setNeutralButton(context.getString(clearResId), null)
            // Set custom layout
            val frameView = FrameLayout(context)
            builder.setView(frameView)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val dialogLayout: View = inflater.inflate(R.layout.dialog_picker_duration, frameView)
            val minutesField: MaterialNumberPicker = dialogLayout.findViewById(R.id.minutesPickerView)
            minutesField.minValue = 0
            minutesField.maxValue = maxAllowedMins
            minutesField.value = mins
            val secondsField: MaterialNumberPicker = dialogLayout.findViewById(R.id.secondsPickerView)
            secondsField.minValue = 0
            secondsField.maxValue = 59
            secondsField.value = seconds
//        minutesField.setOnValueChangedListener((picker, oldVal, newVal) -> {
//            secondsField.setValue(0);
//        });
            alertDialog.setOnShowListener { dialog: DialogInterface ->
                val okBlock = Runnable {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    val m = minutesField.value
                    val s = secondsField.value
                    positiveButtonPublish.onNext(m * 60 + s)
                    dialog.dismiss()
                }
                // Add listeners to dialog buttons
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                positiveBtn.setOnClickListener { okBlock.run() }
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                negativeBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    dialog.dismiss()
                }
                val neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                neutralBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context as Activity)
                    positiveButtonPublish.onNext(0)
                    dialog.dismiss()
                }
            }
            // Show dialog
            alertDialog.show()
            return positiveButtonPublish
        }
    }
}