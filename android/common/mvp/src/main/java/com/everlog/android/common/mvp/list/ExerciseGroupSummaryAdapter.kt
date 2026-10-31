package com.everlog.android.common.mvp.list

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.mvp.R
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.set.summaryValues
import com.everlog.android.common.mvp.databinding.RowExerciseGroupSummaryBinding
import com.everlog.android.common.mvp.databinding.RowExerciseWithinSetSummaryBinding
import com.everlog.android.common.mvp.databinding.RowSetSummaryBinding
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.everlog.android.common.kotlin.format.exerciseSummary
import com.everlog.android.common.kotlin.format.setSummary
import com.everlog.android.common.mvp.view.renderCompleteSetSummary
import com.everlog.android.common.kotlin.Utils
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.R as MvpR
import java.util.*

class ExerciseGroupSummaryAdapter {

    private lateinit var builder: Builder

    fun build(builder: Builder): GroupBinder {
        this.builder = builder
        return GroupBinder()
    }

    class Builder {
        internal var showTemplates = true
        internal var weightUnit = WeightUnit.KILOGRAM
        internal var onExerciseClicked: (Context, ELExercise) -> Unit = { _, _ -> }

        fun setShowTemplates(showTemplates: Boolean): Builder {
            this.showTemplates = showTemplates
            return this
        }

        fun setWeightUnit(weightUnit: WeightUnit): Builder {
            this.weightUnit = weightUnit
            return this
        }

        fun setOnExerciseClicked(onExerciseClicked: (Context, ELExercise) -> Unit): Builder {
            this.onExerciseClicked = onExerciseClicked
            return this
        }
    }

    inner class GroupBinder : ItemBinder<ELExerciseGroup, GroupViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): GroupViewHolder {
            return GroupViewHolder(inflater.inflate(R.layout.row_exercise_group_summary, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELExerciseGroup
        }

        override fun bind(holder: GroupViewHolder, item: ELExerciseGroup) {
            holder.render()
        }
    }

    inner class GroupViewHolder internal constructor(itemView: View) : ItemViewHolder<ELExerciseGroup>(itemView) {

        private val binding = RowExerciseGroupSummaryBinding.bind(itemView)
        private val mAdapter = RecyclerAdapter()
        private val mDataListManager = DataListManager<ELRoutineExercise>(mAdapter)

        init {
            setupListView()
        }

        // Render

        internal fun render() {
            renderSetType()
            renderExercises()
        }

        private fun renderSetType() {
            binding.setTypeLbl.text = String.format("%ss", if (item.getSetType() === ELSetType.SINGLE) "Set" else ArrayResourceTypeUtils.withSetTypes().getTitle(item.type!!, item.type?.lowercase(Locale.getDefault())?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " Set"))
        }

        private fun renderExercises() {
            mDataListManager.set(item.exercises)
            mAdapter.notifyDataSetChanged()
        }

        // Setup

        private fun setupListView() {
            mAdapter.addDataManager(mDataListManager)
            mAdapter.registerBinder(ExerciseBinder())
            binding.recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            binding.recyclerView.adapter = mAdapter
        }
    }

    inner class ExerciseBinder : ItemBinder<ELRoutineExercise, ExerciseViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ExerciseViewHolder {
            return ExerciseViewHolder(inflater.inflate(R.layout.row_exercise_within_set_summary, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELRoutineExercise
        }

        override fun bind(holder: ExerciseViewHolder, item: ELRoutineExercise) {
            holder.render()
        }
    }

    inner class ExerciseViewHolder(itemView: View) : ItemViewHolder<ELRoutineExercise>(itemView) {

        private val binding = RowExerciseWithinSetSummaryBinding.bind(itemView)
        private val mAdapter = RecyclerAdapter()
        private val mDataListManager = DataListManager<ELSet>(mAdapter)

        init {
            setupListView()
        }

        // Render

        internal fun render() {
            renderExercise()
            renderSets()
            // Clicks
            val click = {
                val exercise = item.exercise
                if (Utils.isValidContext(itemView.context) && exercise != null) {
                    builder.onExerciseClicked(itemView.context, exercise)
                }
            }
            binding.exerciseImg.setOnClickListener { click() }
            binding.exerciseContainer.setOnClickListener { click() }
            binding.exerciseContainer.setOnLongClickListener {
                click()
                true
            }
        }

        private fun renderExercise() {
            binding.exerciseName.text = item.getName()
            binding.exerciseImg.setExercise(item.exercise)
            binding.exerciseImg.applyMask(DesignR.drawable.mask_circle)
            binding.exerciseSummary.visibility = if (builder.showTemplates) View.GONE else View.VISIBLE
            if (!builder.showTemplates) {
                val unit = builder.weightUnit
                binding.exerciseSummary.text = exerciseSummary(binding.root.context, item.getTotalWeight(unit), item.getTotalReps(), item.getTotalTimeSeconds(), unit)
            }
        }

        private fun renderSets() {
            mDataListManager.set(item.sets)
            mAdapter.notifyDataSetChanged()
        }

        // Setup

        private fun setupListView() {
            mAdapter.addDataManager(mDataListManager)
            mAdapter.registerBinder(SetBinder())
            binding.recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            binding.recyclerView.adapter = mAdapter
        }
    }

    inner class SetBinder : ItemBinder<ELSet, SetViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): SetViewHolder {
            return SetViewHolder(inflater.inflate(MvpR.layout.row_set_summary, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELSet
        }

        override fun bind(holder: SetViewHolder, item: ELSet) {
            holder.render()
        }
    }

    inner class SetViewHolder(itemView: View) : ItemViewHolder<ELSet>(itemView) {

        private val binding = RowSetSummaryBinding.bind(itemView)

        // Render

        internal fun render() {
            renderNumber()
            if (builder.showTemplates) {
                renderTemplate()
            } else {
                renderCompleteSetSummary(itemView.context, item, builder.weightUnit, binding.setSummary)
            }
        }

        private fun renderNumber() {
            binding.setNumber.text = String.format("%d", absoluteAdapterPosition + 1)
        }

        private fun renderTemplate() {
            val txt = setSummary(itemView.context, item.summaryValues(builder.showTemplates), builder.weightUnit)
            if (TextUtils.isEmpty(txt)) {
                binding.setSummary.text = "Template not set"
                binding.setSummary.setTypeface(null, Typeface.ITALIC)
                binding.setSummary.setTextColor(ContextCompat.getColor(itemView.context, DesignR.color.gray_4))
            } else {
                binding.setSummary.text = txt
                binding.setSummary.setTypeface(null, Typeface.NORMAL)
                binding.setSummary.setTextColor(ContextCompat.getColor(itemView.context, DesignR.color.white_base))
            }
        }
    }
}