package com.everlog.ui.adapters.history

import com.everlog.android.common.design.R as DesignR
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ahamed.multiviewadapter.ItemBinder
import com.everlog.R
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.isSameDay
import com.everlog.android.common.mvp.list.BaseTimelineViewHolder
import com.everlog.android.common.mvp.list.BaseTimelineViewHolder.OnTimelineListener
import com.everlog.android.common.mvp.util.ViewUtils.dpToPx
import com.everlog.android.common.mvp.util.ViewUtils.getRawDimension
import com.everlog.android.common.mvp.util.ViewUtils.setMargins
import com.everlog.databinding.RowHistoryBinding
import com.everlog.utils.format.summary
import java.util.*

class HistoryAdapter {

    class Binder(private val mListener: OnTimelineListener<ELWorkout>) : ItemBinder<ELWorkout, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_history, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELWorkout
        }

        override fun bind(holder: ViewHolder, item: ELWorkout) {
            holder.setListener(mListener)
            holder.render()
            holder.mRoutineContainer?.setOnClickListener { mListener.onItemClicked(item, holder.absoluteAdapterPosition) }
        }
    }

    class ViewHolder(itemView: View) : BaseTimelineViewHolder<ELWorkout>(itemView) {

        private val binding = RowHistoryBinding.bind(itemView)
        val mRoutineContainer = binding.routinePicker.routineContainer

        override fun getDate(): Date {
            return item.getCompletedDateAsDate()
        }

        // Render

        override fun render() {
            super.render()
            renderRoutine()
            renderGroups()
        }

        private fun renderRoutine() {
            binding.routinePicker.routineName.text = item.getName()
            binding.routinePicker.routineSummary.text = item.summary(binding.root.context)
        }

        private fun renderGroups() {
            if (getDate().isSameDay(getListener()?.dateOfNextItem(absoluteAdapterPosition) ?: 0)) {
                // Do not add bottom padding if the workouts are in the same day
                val elevation = dpToPx(getRawDimension(itemView.context, DesignR.dimen.card_elevation_default))
                setMargins(binding.routinePicker.routineContainer, 0, 0, 0, elevation)
            } else {
                // Add bottom padding if the workouts are not in the same day
                val margin = dpToPx(getRawDimension(itemView.context, DesignR.dimen.margin_12))
                setMargins(binding.routinePicker.routineContainer, 0, 0, 0, margin)
            }
        }
    }
}