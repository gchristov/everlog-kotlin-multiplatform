package com.everlog.ui.dialog

import android.app.Dialog
import android.content.Context
import com.everlog.R
import com.everlog.managers.auth.LocalUserManager
import rx.Observable

internal class AppBlockerDialogs {

    companion object {

        private var mDialog: Dialog? = null

        @JvmStatic
        fun showAppBlockerDialog(context: Context, type: DialogBuilder.AppBlockerDialogType): Observable<Int> {
            try {
                mDialog?.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            var title = ""
            var message = ""
            var yes = ""
            var no = ""
            when (type) {
                DialogBuilder.AppBlockerDialogType.NEWSLETTER -> {
                    title = context.getString(R.string.notifications_newsletter_title)
                    message = context.getString(R.string.notifications_newsletter_prompt, LocalUserManager.getUser()!!.getFirstName())
                    yes = context.getString(R.string.notifications_newsletter_prompt_yes)
                    no = context.getString(R.string.rate_no)
                }
            }
            val data = DialogBuilder.buildPrompt(context, title, message, yes, no)
            data.second.setCancelable(false)
            data.second.show()
            mDialog = data.second
            return data.first
        }
    }
}