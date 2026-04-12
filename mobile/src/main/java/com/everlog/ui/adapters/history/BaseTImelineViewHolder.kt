package com.everlog.ui.adapters.history

import android.view.View
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.databinding.IncludeTimelineBinding
import com.everlog.ui.adapters.OnListItemListener
import com.everlog.utils.isSameDay
import com.everlog.utils.toLocalDateTime
import org.threeten.bp.format.TextStyle
import java.util.*

abstract class BaseTimelineViewHolder<T : Any>(itemView: View) : ItemViewHolder<T>(itemView) {

    protected val timelineBinding = IncludeTimelineBinding.bind(itemView)

    abstract fun getDate(): Date

    private var mListener: OnTimelineListener<T>? = null

    fun setListener(listener: OnTimelineListener<T>?) {
        mListener = listener
    }

    fun getListener(): OnTimelineListener<T>? {
        return mListener
    }

    open fun render() {
        renderDate()
        renderTimeline()
    }

    // Render

    private fun renderDate() {
        val date = getDate().toLocalDateTime()
        timelineBinding.dayTimeline.text = String.format("%d", date.dayOfMonth)
        timelineBinding.monthTimeline.text = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        timelineBinding.yearTimeline.text = String.format("%d", date.year)
    }

    private fun renderTimeline() {
        if (getDate().isSameDay(mListener?.dateOfPreviousItem(absoluteAdapterPosition) ?: 0)) {
            timelineBinding.dayTimeline.visibility = View.INVISIBLE
            timelineBinding.monthTimeline.visibility = View.INVISIBLE
            timelineBinding.yearTimeline.visibility = View.INVISIBLE
        } else {
            timelineBinding.dayTimeline.visibility = View.VISIBLE
            timelineBinding.monthTimeline.visibility = View.VISIBLE
            timelineBinding.yearTimeline.visibility = View.VISIBLE
        }
        timelineBinding.topTimeline.visibility = if (isAtTop) View.INVISIBLE else View.VISIBLE
        timelineBinding.bottomTimeline.visibility = if (isAtBottom) View.INVISIBLE else View.VISIBLE
    }

    private val isAtTop: Boolean
        get() = absoluteAdapterPosition == 0
    private val isAtBottom: Boolean
        get() = absoluteAdapterPosition >= (mListener?.collectionSize() ?: 0) - 1

    interface OnTimelineListener<T> : OnListItemListener<T> {
        fun collectionSize(): Int
        fun dateOfPreviousItem(position: Int): Long?
        fun dateOfNextItem(position: Int): Long?
    }
}