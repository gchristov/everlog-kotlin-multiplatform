package com.everlog.ui.adapters.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.R
import com.everlog.data.model.plan.ELPlan
import com.everlog.databinding.RowPlanBinding
import com.everlog.ui.adapters.OnListItemListener
import com.everlog.utils.glide.ELGlideModule

class PlanAdapter {

    class Binder(private val listener: OnPlanListener) : ItemBinder<ELPlan, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_plan, parent, false), listener)
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELPlan
        }

        override fun bind(holder: ViewHolder, item: ELPlan) {
            holder.render()
        }
    }

    class ViewHolder(itemView: View, private val listener: OnPlanListener) : ItemViewHolder<ELPlan>(itemView) {

        private val binding = RowPlanBinding.bind(itemView)

        // Render

        fun render() {
            renderPlan(item)
            renderImage()
            // Clicks
            itemView.setOnClickListener { listener.onItemClicked(item, absoluteAdapterPosition) }
        }

        private fun renderPlan(item: ELPlan) {
            binding.titleField.text = item.name
            val weeks = item.weeks.size
            binding.subtitleField.text = itemView.context.resources.getQuantityString(R.plurals.weeks, weeks, weeks)
        }

        private fun renderImage() {
            if (item.imageUrl != null) {
                ELGlideModule.loadImage(item.imageUrl, binding.imageView)
            } else {
                binding.imageView.setImageDrawable(null)
            }
        }
    }

    interface OnPlanListener : OnListItemListener<ELPlan>
}