package com.everlog.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.databinding.RowCalendarDayBinding
import com.everlog.databinding.RowCalendarMonthBinding
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.buildWeekDays
import com.everlog.utils.fromJava
import com.everlog.utils.toJava
import com.kizitonwose.calendarview.CalendarView
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import org.threeten.bp.LocalDate
import java.util.*
import kotlin.collections.ArrayList

class CalendarAdapter {

    // TODO: Months

    private lateinit var monthBuilder: MonthBuilder
    private val now = LocalDate.now().toJava()
    private var selectedDate = now.fromJava()

    fun buildMonth(builder: MonthBuilder): MonthBinder {
        this.monthBuilder = builder
        return MonthBinder()
    }

    class MonthBuilder {
        internal var calendar: CalendarView? = null
        fun calendar(calendar: CalendarView): MonthBuilder {
            this.calendar = calendar
            return this
        }
    }

    inner class MonthBinder : MonthHeaderFooterBinder<MonthViewHolder> {

        override fun create(view: View): MonthViewHolder {
            return MonthViewHolder(view)
        }

        override fun bind(container: MonthViewHolder, month: CalendarMonth) {
            container.setItem(month)
        }
    }

    inner class MonthViewHolder constructor(itemView: View) : ViewContainer(itemView) {

        val binding = RowCalendarMonthBinding.bind(itemView)

        private lateinit var mItem: CalendarMonth
        private val mWeekDayViews = ArrayList<TextView>()

        init {
            setupWeekDays()
        }

        internal fun setItem(item: CalendarMonth) {
            this.mItem = item
            renderDate()
        }

        // Render

        private fun renderDate() {
            @SuppressLint("SetTextI18n")
            binding.calendarMonthText.text = "${mItem.yearMonth.month?.name?.lowercase(Locale.getDefault())?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${mItem.year}"
        }

        // Setup

        private fun setupWeekDays() {
            val days = SettingsManager.manager.firstDayOfWeek().buildWeekDays()
            days.forEach {
                val textView = TextView(view.context)
                textView.gravity = Gravity.CENTER
                textView.text = "" + it.day?.first()
                textView.isAllCaps = true
                textView.setTextColor(ContextCompat.getColor(view.context, R.color.gray_4))
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                mWeekDayViews.add(textView)
                binding.calendarDaysOfWeek.addView(textView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
    }

    // TODO: Days

    private lateinit var dayBuilder: DayBuilder

    fun buildDay(builder: DayBuilder): DayBinder {
        this.dayBuilder = builder
        return DayBinder()
    }

    class DayBuilder {
        internal var calendar: CalendarView? = null
        internal var listener: OnCalendarListener? = null
        fun calendar(calendar: CalendarView): DayBuilder {
            this.calendar = calendar
            return this
        }
        fun listener(listener: OnCalendarListener): DayBuilder {
            this.listener = listener
            return this
        }
    }

    inner class DayBinder : com.kizitonwose.calendarview.ui.DayBinder<DayViewHolder> {

        override fun create(view: View): DayViewHolder {
            return DayViewHolder(view)
        }

        override fun bind(container: DayViewHolder, day: CalendarDay) {
            container.setItem(day)
        }
    }

    inner class DayViewHolder constructor(itemView: View) : ViewContainer(itemView) {

        val binding = RowCalendarDayBinding.bind(itemView)

        private lateinit var mItem: CalendarDay

        init {
            (itemView.parent as? ViewGroup)?.clipChildren = false
            (itemView.parent as? ViewGroup)?.clipToPadding = false
            (itemView.parent?.parent as? ViewGroup)?.clipChildren = false
            (itemView.parent?.parent as? ViewGroup)?.clipToPadding = false
        }

        internal fun setItem(item: CalendarDay) {
            this.mItem = item
            renderDate()
            // Clicks
            binding.calendarDayContainer.setOnClickListener {
                if (isOwnerThisMonth() && !isInFuture()) {
                    val oldDate = selectedDate.toJava()
                    selectedDate = LocalDate.of(item.date.year, item.date.month.value, item.date.dayOfMonth)
                    dayBuilder.calendar?.notifyDateChanged(item.date)
                    oldDate.let { dayBuilder.calendar?.notifyDateChanged(oldDate) }
                    dayBuilder.listener?.onDateSelected(item.date.fromJava())
                }
            }
        }

        // Render

        private fun renderDate() {
            binding.calendarDayEvent.visibility = if (dayBuilder.listener?.hasEvents(mItem.date.fromJava()) == true) View.VISIBLE else View.INVISIBLE
            binding.calendarDayMonthText.visibility = View.GONE
            binding.calendarDayMonthText.text = mItem.date.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            binding.calendarDayText.text = mItem.date.dayOfMonth.toString()
            binding.calendarDayText.setTextColor(ContextCompat.getColor(view.context, R.color.white_base))
            binding.calendarDayText.setTypeface(null, Typeface.NORMAL)
            binding.calendarDayContainer.visibility = if (isOwnerThisMonth()) View.VISIBLE else View.INVISIBLE
            binding.calendarDayContainer.alpha = 1f
            binding.calendarDayToday.visibility = View.GONE
            binding.calendarDaySelected.visibility = View.GONE
            when {
                isSelected() || isToday() -> {
                    // Selected or Today
                    binding.calendarDayContainer.alpha = 1f
                    binding.calendarDayToday.visibility = View.GONE
                    if (isToday()) binding.calendarDayToday.visibility = View.VISIBLE
                    if (isSelected()) {
                        binding.calendarDaySelected.visibility = View.VISIBLE
                        binding.calendarDayMonthText.visibility = View.VISIBLE
                        binding.calendarDayText.setTextColor(ContextCompat.getColor(view.context, R.color.background_card))
                        binding.calendarDayText.setTypeface(null, Typeface.BOLD)
                    }
                }
                isInFuture() -> {
                    // Future
                    binding.calendarDayContainer.alpha = 0.5f
                }
            }
        }

        private fun isOwnerThisMonth(): Boolean {
            return mItem.owner == DayOwner.THIS_MONTH
        }

        private fun isInFuture(): Boolean {
            return mItem.date.isAfter(now)
        }

        private fun isSelected(): Boolean {
            return mItem.date == selectedDate.toJava()
        }

        private fun isToday(): Boolean {
            return mItem.date == now
        }
    }

    interface OnCalendarListener {
        fun onDateSelected(date: LocalDate)
        fun hasEvents(date: LocalDate): Boolean
    }
}
