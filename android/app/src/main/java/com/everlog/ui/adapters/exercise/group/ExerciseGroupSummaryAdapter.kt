package com.everlog.ui.adapters.exercise.group

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.databinding.RowExerciseGroupSummaryBinding
import com.everlog.databinding.RowExerciseWithinSetSummaryBinding
import com.everlog.databinding.RowSetSummaryBinding
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.Utils
import java.util.*
import kotlin.math.max

class ExerciseGroupSummaryAdapter {

    private lateinit var builder: Builder

    companion object {

        @JvmStatic
        fun renderCompleteSetSummary(context: Context,
                                     set: ELSet,
                                     summaryView: TextView?) {
            val txt = set.getWorkoutDetailsSummary(context) ?: ""
            if (set.isWithoutData()) {
                summaryView?.text = txt
                summaryView?.setTypeface(null, Typeface.ITALIC)
                summaryView?.setTextColor(ContextCompat.getColor(context, R.color.gray_4))
            } else {
                val str = SpannableStringBuilder(txt)
                val index = if (txt.contains("x")) txt.indexOf("x") else txt.indexOf("•")
                val xPos = max(index + 1, 0)
                str.setSpan(StyleSpan(Typeface.BOLD), xPos, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                str.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.white_base)), xPos, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                str.setSpan(StyleSpan(Typeface.NORMAL), 0, xPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                str.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray_1)), 0, xPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                summaryView?.text = str
                summaryView?.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    fun build(builder: Builder): GroupBinder {
        this.builder = builder
        return GroupBinder()
    }

    class Builder {
        internal var showTemplates = true
        fun setShowTemplates(showTemplates: Boolean): Builder {
            this.showTemplates = showTemplates
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
                if (Utils.isValidContext(itemView.context) && item.exercise != null) {
                    ELNavigator(itemView.context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties()
                            .exercise(item.exercise!!))
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
            binding.exerciseImg.applyMask(R.drawable.mask_circle)
            binding.exerciseSummary.visibility = if (builder.showTemplates) View.GONE else View.VISIBLE
            if (!builder.showTemplates) {
                binding.exerciseSummary.text = item.getSummary()
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
            return SetViewHolder(inflater.inflate(R.layout.row_set_summary, parent, false))
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
                renderCompleteSetSummary(itemView.context, item, binding.setSummary)
            }
        }

        private fun renderNumber() {
            binding.setNumber.text = String.format("%d", absoluteAdapterPosition + 1)
        }

        private fun renderTemplate() {
            val txt = item.getExerciseSetSummary(itemView.context, builder.showTemplates)
            if (TextUtils.isEmpty(txt)) {
                binding.setSummary.text = "Template not set"
                binding.setSummary.setTypeface(null, Typeface.ITALIC)
                binding.setSummary.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_4))
            } else {
                binding.setSummary.text = txt
                binding.setSummary.setTypeface(null, Typeface.NORMAL)
                binding.setSummary.setTextColor(ContextCompat.getColor(itemView.context, R.color.white_base))
            }
        }
    }
}