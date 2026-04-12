package com.everlog.ui.adapters.plan

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.R
import com.everlog.data.model.plan.ELPlanWeek
import com.everlog.databinding.RowPlanWeekBinding
import com.everlog.ui.adapters.OnListItemListener

class PlanWeekAdapter {

    class Binder(private val listener: OnPlanWeekListener) : ItemBinder<ELPlanWeek, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_plan_week, parent, false), listener)
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELPlanWeek
        }

        override fun bind(holder: ViewHolder, item: ELPlanWeek) {
            holder.render()
        }
    }

    class ViewHolder(itemView: View, private val listener: OnPlanWeekListener) : ItemViewHolder<ELPlanWeek>(itemView) {

        private val binding = RowPlanWeekBinding.bind(itemView)

        private fun openMenu(context: Context) {
            val popup = PopupMenu(context, binding.menuBtn)
            popup.inflate(R.menu.menu_row_plan_week)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_duplicate -> listener.onClickDuplicate(item, absoluteAdapterPosition)
                    R.id.action_delete -> listener.onClickDelete(item, absoluteAdapterPosition)
                }
                false
            }
            val menuHelper = MenuPopupHelper(itemView.context, (popup.menu as MenuBuilder), binding.menuBtn)
            menuHelper.setForceShowIcon(true)
            menuHelper.show()
        }

        // Render

        fun render() {
            renderWeek()
            // Clicks
            itemView.setOnClickListener {
                listener.onItemClicked(item, absoluteAdapterPosition)
            }
            binding.menuBtn.setOnClickListener {
                openMenu(it.context)
            }
        }

        private fun renderWeek() {
            val workouts = item.getTotalWorkouts()
            binding.titleField.text = String.format("Week %d", absoluteAdapterPosition + 1)
            binding.detailsLbl.text = if (item.isValid()) itemView.context.resources.getQuantityString(R.plurals.workouts, workouts, workouts) else itemView.context.getText(R.string.tap_to_setup)
            binding.detailsLbl.setTypeface(null, if (item.isValid()) Typeface.NORMAL else Typeface.ITALIC)
            binding.detailsLbl.setTextColor(ContextCompat.getColor(itemView.context, if (item.isValid()) R.color.gray_5 else R.color.gray_3))
        }
    }

    interface OnPlanWeekListener : OnListItemListener<ELPlanWeek> {
        fun onClickDuplicate(week: ELPlanWeek, position: Int)
        fun onClickDelete(week: ELPlanWeek, position: Int)
    }
}