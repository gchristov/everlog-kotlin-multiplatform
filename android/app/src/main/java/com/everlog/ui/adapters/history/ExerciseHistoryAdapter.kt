package com.everlog.ui.adapters.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.data.model.set.ELSet
import com.everlog.databinding.RowHistoryExerciseBinding
import com.everlog.databinding.RowSetSummaryBinding
import com.everlog.ui.adapters.exercise.group.ExerciseGroupSummaryAdapter.Companion.renderCompleteSetSummary
import com.everlog.ui.adapters.history.BaseTimelineViewHolder.OnTimelineListener
import java.util.Date

class ExerciseHistoryAdapter {

    class Binder(private val mListener: OnTimelineListener<ELExerciseHistory>) : ItemBinder<ELExerciseHistory, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_history_exercise, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELExerciseHistory
        }

        override fun bind(holder: ViewHolder, item: ELExerciseHistory) {
            holder.setListener(mListener)
            holder.render()
        }
    }

    class ViewHolder(itemView: View) : BaseTimelineViewHolder<ELExerciseHistory>(itemView) {

        private var mSetBinder = SetBinder()
        private val binding = RowHistoryExerciseBinding.bind(itemView)
        private val mAdapter = RecyclerAdapter()
        private val mDataListManager = DataListManager<ELSet>(mAdapter)

        init {
            setupListView()
        }

        override fun getDate(): Date {
            return item.workout?.getCompletedDateAsDate() ?: Date()
        }

        // Render

        override fun render() {
            super.render()
            renderExercise()
            renderSets()
        }

        private fun renderSets() {
            mDataListManager.set(item.exercise?.getSetsWithData())
            mAdapter.notifyDataSetChanged()
        }

        private fun renderExercise() {
            binding.routineName.text = item.workout?.getName()
            binding.exerciseSummary.text = item.exercise?.getSummary()
        }

        // Setup

        private fun setupListView() {
            mAdapter.addDataManager(mDataListManager)
            mAdapter.registerBinder(mSetBinder)
            binding.recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            binding.recyclerView.adapter = mAdapter
        }
    }

    internal class SetBinder : ItemBinder<ELSet, SetViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): SetViewHolder {
            return SetViewHolder(inflater.inflate(R.layout.row_set_summary, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELSet
        }

        override fun bind(holder: SetViewHolder, item: ELSet) {
            holder.render()
        }
    }

    internal class SetViewHolder(itemView: View) : ItemViewHolder<ELSet>(itemView) {

        private val binding = RowSetSummaryBinding.bind(itemView)

        // Render

        fun render() {
            binding.setNumber.text = String.format("%d", absoluteAdapterPosition + 1)
            renderCompleteSetSummary(itemView.context, item, binding.setSummary)
        }
    }
}