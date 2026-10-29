package com.everlog.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.everlog.R
import com.everlog.ui.dialog.DialogBuilder.MultipleChoiceDialogType
import com.everlog.utils.Utils
import rx.Observable
import rx.subjects.PublishSubject

internal class MultipleChoiceDialogs {

    companion object {

        @JvmStatic
        fun showPickerMultipleChoiceDialog(context: Context,
                                           options: Array<String>,
                                           selectedIndex: Int,
                                           type: MultipleChoiceDialogType): Observable<Int> {
            val valuePublish = PublishSubject.create<Int>()
            if (!Utils.isValidContext(context)) {
                return valuePublish
            }
            var titleResId = 0
            when (type) {
                MultipleChoiceDialogType.UNIT_WEIGHT -> titleResId = R.string.settings_unit_weight
                MultipleChoiceDialogType.FIRST_DAY_OF_WEEK -> titleResId = R.string.settings_first_week_day
                MultipleChoiceDialogType.EXERCISE_CATEGORY -> titleResId = R.string.create_exercise_select_category
            }

            // Create dialog.
            val builder = AlertDialog.Builder(context, R.style.DarkDialogTheme_RadioGroup)
            builder.setTitle(titleResId)

            // Attach buttons.
            builder.setSingleChoiceItems(options, selectedIndex) { dialog: DialogInterface, item: Int ->
                if (selectedIndex != item) {
                    valuePublish.onNext(item)
                    Utils.runWithDelay({ dialog.dismiss() }, 500)
                } else {
                    dialog.dismiss()
                }
            }
            val alert = builder.create()
            alert.show()
            return valuePublish
        }
    }
}