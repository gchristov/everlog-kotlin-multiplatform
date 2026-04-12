package com.everlog.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.dialog.DialogBuilder.MetricDialogType
import com.everlog.ui.dialog.DialogBuilder.StringDialogType
import com.everlog.utils.Utils
import com.everlog.utils.input.KeyboardUtils
import rx.Observable
import rx.subjects.PublishSubject

internal class InputDialogs {

    companion object {

        @JvmStatic
        fun showInputStringDialog(context: Context,
                                  currentValue: String?,
                                  type: StringDialogType): Observable<String> {
            val positiveButtonPublish = PublishSubject.create<String>()
            if (!Utils.isValidContext(context)) {
                return positiveButtonPublish
            }
            var title = ""
            var layoutResId = 0
            when (type) {
                StringDialogType.ROUTINE_NAME, StringDialogType.WORKOUT_NAME -> {
                    title = context.getString(R.string.create_routine_edit_name_title)
                    layoutResId = R.layout.dialog_input_string
                }
                StringDialogType.WORKOUT_NOTE -> {
                    title = context.getString(R.string.notes)
                    layoutResId = R.layout.dialog_input_string_multiline
                }
            }

            // Create dialog.
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme)
            builder.setTitle(title)

            // Attach buttons.
            builder
                    .setPositiveButton(context.getString(R.string.save), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
            if (type == StringDialogType.WORKOUT_NOTE) {
                builder.setNeutralButton(context.getString(R.string.clear), null)
            }

            // Set custom layout.
            val frameView = FrameLayout(context)
            builder.setView(frameView)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val dialogLayout = inflater.inflate(layoutResId, frameView)
            val inputField = dialogLayout.findViewById<EditText>(R.id.nameField)
            inputField.setText(currentValue ?: "")
            inputField.setSelectAllOnFocus(true)
            alertDialog.setOnShowListener { dialog: DialogInterface ->
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                positiveBtn.setOnClickListener {
                    val text = inputField.text.toString().trim { it <= ' ' }
                    if (!TextUtils.isEmpty(text)) {
                        KeyboardUtils.hideKeyboard(context, inputField)
                        positiveButtonPublish.onNext(text)
                        dialog.dismiss()
                    } else {
                        ToastBuilder.showToast(context, context.getString(R.string.create_routine_error_no_title))
                    }
                }
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                negativeBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context, inputField)
                    dialog.dismiss()
                }
                val neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                neutralBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context, inputField)
                    positiveButtonPublish.onNext("")
                    dialog.dismiss()
                }
            }

            // Show dialog.
            inputField.onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                val window = alertDialog.window
                if (hasFocus && window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
            alertDialog.show()
            inputField.requestFocus()
            return positiveButtonPublish
        }

        @JvmStatic
        fun showInputNumberDialog(context: Context,
                                  value: String?,
                                  type: MetricDialogType): Observable<String> {
            val positiveButtonPublish = PublishSubject.create<String>()
            if (!Utils.isValidContext(context)) {
                return positiveButtonPublish
            }
            var title = ""
            var inputType = InputType.TYPE_CLASS_NUMBER
            when (type) {
                MetricDialogType.WEIGHT -> {
                    title = String.format("%s (%s)", context.getString(R.string.exercise_sets_performed_weight), SettingsManager.weightUnitAbbreviation())
                    inputType = inputType or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                MetricDialogType.REPS -> {
                    title = context.getString(R.string.exercise_sets_performed_reps)
                }
                MetricDialogType.REPS_REQUIRED -> {
                    title = context.getString(R.string.exercise_sets_required_reps)
                }
            }

            // Create dialog.
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme)
            builder.setTitle(title)

            // Attach buttons.
            builder.setPositiveButton(context.getString(R.string.save), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .setNeutralButton(context.getString(R.string.clear), null)

            // Set custom layout.
            val frameView = FrameLayout(context)
            builder.setView(frameView)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val dialogLayout: View = inflater.inflate(R.layout.dialog_input_number, frameView)
            val inputField = dialogLayout.findViewById<EditText>(R.id.metricField)
            inputField.setText(value ?: "")
            inputField.setSelectAllOnFocus(true)
            inputField.inputType = inputType
            alertDialog.setOnShowListener { dialog: DialogInterface ->
                val okBlock = Runnable {
                    val text = inputField.text.toString().trim { it <= ' ' }
                    if (!TextUtils.isEmpty(text)) {
                        KeyboardUtils.hideKeyboard(context, inputField)
                        positiveButtonPublish.onNext(text)
                        dialog.dismiss()
                    } else {
                        ToastBuilder.showToast(context, context.getString(R.string.workout_metric_prompt_missing))
                    }
                }
                // Add OK listener to keyboard.
                inputField.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        okBlock.run()
                    }
                    false
                }
                // Add listeners to dialog buttons.
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                positiveBtn.setOnClickListener { okBlock.run() }
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                negativeBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context, inputField)
                    dialog.dismiss()
                }
                val neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralBtn.setTextColor(ContextCompat.getColor(context, R.color.main_accent))
                neutralBtn.setOnClickListener {
                    KeyboardUtils.hideKeyboard(context, inputField)
                    positiveButtonPublish.onNext("")
                    dialog.dismiss()
                }
            }

            // Show dialog.
            inputField.onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                val window = alertDialog.window
                if (hasFocus && window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
            alertDialog.show()
            inputField.requestFocus()
            return positiveButtonPublish
        }
    }
}