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
import com.everlog.constants.ELConstants
import com.everlog.data.model.plan.ELPlanDay
import com.everlog.databinding.RowPlanDayBinding
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.adapters.OnListItemListener
import com.everlog.ui.navigator.ELNavigator

class PlanDayAdapter {

    class Binder(private val mListener: OnPlanDayListener) : ItemBinder<ELPlanDay, ViewHolder>() {

        private var ongoing = false
        private var edit = false

        fun setOngoing(ongoing: Boolean): Binder {
            this.ongoing = ongoing
            return this
        }

        fun setEdit(edit: Boolean): Binder {
            this.edit = edit
            return this
        }

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_plan_day, parent, false), ongoing, edit, mListener)
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELPlanDay
        }

        override fun bind(holder: ViewHolder, item: ELPlanDay) {
            holder.render()
        }
    }

    class ViewHolder(itemView: View,
                     private val ongoing: Boolean,
                     private val edit: Boolean,
                     private val listener: OnPlanDayListener) : ItemViewHolder<ELPlanDay>(itemView) {

        private val binding = RowPlanDayBinding.bind(itemView)

        init {
            binding.skipBtn.setOnClickListener { onClickSkip() }
            binding.startBtn.setOnClickListener { onClickStart() }
        }

        fun onClickSkip() {
            listener.onClickSkip(item)
        }

        fun onClickStart() {
            proRunLocked { listener.onClickStart(item) }
        }

        private fun openMenu(context: Context) {
            val popup = PopupMenu(context, binding.planMenuBtn)
            popup.inflate(R.menu.menu_row_plan_day)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_start_routine -> listener.onClickChooseRoutine(item, absoluteAdapterPosition)
                    R.id.action_rest -> {
                        item.setRest(true)
                        listener.onDayEdited()
                        AnalyticsManager.manager.planWeekDaySetRest()
                        renderDay()
                    }
                }
                false
            }
            val menuHelper = MenuPopupHelper(binding.planMenuBtn.context, (popup.menu as MenuBuilder), binding.planMenuBtn)
            menuHelper.setForceShowIcon(true)
            menuHelper.show()
        }

        private fun proRunLocked(block: Runnable) {
            if (item.proLocked) ELNavigator(itemView.context).runProFeature(block) else block.run()
        }

        // Render

        fun render() {
            renderCompletedState()
            renderDay()
            renderButtons()
            renderPro()
            // Clicks
            itemView.setOnClickListener {
                proRunLocked {
                    if (edit) {
                        openMenu(itemView.context)
                    } else {
                        listener.onItemClicked(item, absoluteAdapterPosition)
                    }
                }
            }
        }

        private fun renderCompletedState() {
            binding.completeContainer.visibility = if (ongoing && item.complete) View.VISIBLE else View.GONE
            binding.incompleteContainer.visibility = if (!ongoing || !item.complete) View.VISIBLE else View.GONE
            binding.incompleteCheck.visibility = if (ongoing) View.VISIBLE else View.GONE
        }

        private fun renderDay() {
            binding.dayFieldComplete.text = String.format("Day %d", absoluteAdapterPosition + 1)
            binding.dayFieldIncomplete.text = binding.dayFieldComplete.text
            if (edit && item.isEmpty()) {
                binding.dayInfoFieldComplete.setText(R.string.tap_to_setup)
                binding.dayInfoFieldComplete.setTypeface(null, Typeface.ITALIC)
                binding.dayInfoFieldComplete.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_3))
            } else {
                binding.dayInfoFieldComplete.text = if (item.getRoutine() != null) item.getRoutine()?.name else itemView.context.getText(R.string.plan_details_rest_title)
                binding.dayInfoFieldComplete.setTypeface(null, Typeface.NORMAL)
                binding.dayInfoFieldComplete.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_5))
            }
            binding.dayInfoFieldIncomplete.text = binding.dayInfoFieldComplete.text
            binding.dayInfoFieldIncomplete.typeface = binding.dayInfoFieldComplete.typeface
            binding.dayInfoFieldIncomplete.setTextColor(binding.dayInfoFieldComplete.textColors)
        }

        private fun renderButtons() {
            var showButtons = false
            if (ongoing && !item.complete) {
                showButtons = item.next
            }
            binding.buttonsLayout.visibility = if (showButtons) View.VISIBLE else View.GONE
            binding.startBtn.setText(if (item.getRest() || item.isEmpty()) R.string.complete else R.string.home_week_plan_start)
            binding.planMenuBtn.visibility = if (edit) View.VISIBLE else View.GONE
        }

        private fun renderPro() {
            // Complete
            binding.proBadgeComplete.visibility = if (item.proLocked) View.VISIBLE else View.GONE
            binding.dayFieldComplete.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            binding.dayInfoFieldComplete.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            // Incomplete
            binding.proBadgeIncomplete.visibility = if (item.proLocked) View.VISIBLE else View.GONE
            binding.dayFieldIncomplete.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            binding.dayInfoFieldIncomplete.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            binding.startBtn.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            // Common
            binding.planMenuBtn.alpha = if (item.proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            if (item.proLocked) {
                binding.dayInfoFieldComplete.setText(R.string.tap_to_setup_upgrade)
                binding.dayInfoFieldIncomplete.text = binding.dayInfoFieldComplete.text
            }
        }
    }

    interface OnPlanDayListener : OnListItemListener<ELPlanDay> {
        fun onClickChooseRoutine(day: ELPlanDay, position: Int)
        fun onClickSkip(day: ELPlanDay)
        fun onClickStart(day: ELPlanDay)
        fun onDayEdited()
    }
}