package com.everlog.android.common.mvp.dialog

import android.app.AlertDialog
import android.content.Context
import com.everlog.android.common.kotlin.Utils
import rx.Observable
import rx.subjects.PublishSubject
import com.everlog.android.common.design.R as DesignR

/**
 * A list to pick one option from, with [selectedIndex] ticked (-1 for none). Emits the picked option's index,
 * nothing when the ticked one is picked again or the dialog is closed.
 */
fun showChoiceList(context: Context, titleResId: Int, options: Array<String>, selectedIndex: Int): Observable<Int> {
    val picked = PublishSubject.create<Int>()
    if (!Utils.isValidContext(context)) {
        return picked
    }
    AlertDialog.Builder(context, DesignR.style.DarkDialogTheme_RadioGroup)
        .setTitle(titleResId)
        .setSingleChoiceItems(options, selectedIndex) { dialog, item ->
            if (selectedIndex != item) {
                picked.onNext(item)
                // Lets the tick show before the dialog goes
                Utils.runWithDelay({ dialog.dismiss() }, 500)
            } else {
                dialog.dismiss()
            }
        }
        .create()
        .show()
    return picked
}
