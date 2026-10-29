package com.everlog.ui.adapters.exercise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.R
import com.everlog.databinding.RowExerciseCategoryBinding
import com.everlog.ui.views.revealcircle.FilterExercisesView.ExerciseCategory

class ExerciseCategoryAdapter {

    class Binder : ItemBinder<ExerciseCategory, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_exercise_category, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ExerciseCategory
        }

        override fun bind(holder: ViewHolder, item: ExerciseCategory) {
            holder.render()
        }
    }

    class ViewHolder(itemView: View) : ItemViewHolder<ExerciseCategory>(itemView) {

        private val binding = RowExerciseCategoryBinding.bind(itemView)

        // Render

        fun render() {
            renderCategory()
            renderSelection()
            // Clicks
            itemView.setOnClickListener {
                item.selected = !item.selected
                renderSelection()
            }
        }

        private fun renderCategory() {
            binding.title.text = item.title
        }

        private fun renderSelection() {
            binding.title.setTextColor(ContextCompat.getColor(itemView.context, if (item.selected) R.color.background_card else R.color.white_base))
            binding.title.setBackgroundResource(if (item.selected) R.drawable.rounded_corners_exercise_muscle_group_selected else R.drawable.rounded_corners_exercise_muscle_group_unselected)
        }
    }
}