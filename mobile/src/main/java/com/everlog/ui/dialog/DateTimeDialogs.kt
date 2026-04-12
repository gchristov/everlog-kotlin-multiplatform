package com.everlog.ui.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.TimePicker
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.toLocalDateTime
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import rx.Observable
import rx.subjects.PublishSubject
import java.util.*

class DateTimeDialogs {

    companion object {

        private var datePickerDialog: DatePickerDialog? = null
        private var timePickerDialog: TimePickerDialog? = null

        @JvmStatic
        fun showDateDialog(context: Context, date: Date?): Observable<Date> {
            val subscriber = PublishSubject.create<Date>()
            if (datePickerDialog != null && datePickerDialog!!.isShowing) {
                return subscriber
            }
            val usedDate = date?.toLocalDateTime() ?: LocalDateTime.now()
            datePickerDialog = DatePickerDialog(context,
                    DatePickerDialog.OnDateSetListener { _: DatePicker?, year: Int, month: Int, day: Int ->
                        showTimeDialog(context, usedDate.withYear(year).withMonth(month + 1).withDayOfMonth(day), subscriber)
                    },
                    usedDate.year,
                    usedDate.monthValue - 1,
                    usedDate.dayOfMonth)
            // Set first day of week (offset due to differences in ordinals)
            datePickerDialog?.datePicker?.firstDayOfWeek = SettingsManager.manager.firstDayOfWeek().plus(1).value
            datePickerDialog?.show()
            return subscriber
        }

        private fun showTimeDialog(context: Context,
                                   usedDate: LocalDateTime,
                                   subscriber: PublishSubject<Date>) {
            if (timePickerDialog != null && timePickerDialog!!.isShowing) {
                return
            }
            timePickerDialog = TimePickerDialog(context,
                    TimePickerDialog.OnTimeSetListener { _: TimePicker?, hour: Int, minute: Int ->
                        val date = DateTimeUtils.toDate(usedDate
                                .withHour(hour)
                                .withMinute(minute)
                                .atZone(ZoneId.systemDefault()).toInstant())
                        subscriber.onNext(date)
                    },
                    usedDate.hour,
                    usedDate.minute,
                    true)
            timePickerDialog?.show()
        }
    }
}