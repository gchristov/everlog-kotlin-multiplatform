package com.everlog.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.RowSettingsMuscleGoalBinding
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.MuscleGoal

class MuscleGoalAdapter {

    class Binder(private val listener: OnListItemListener<MuscleGoal>) : ItemBinder<MuscleGoal, ViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_settings_muscle_goal, parent, false), listener)
        }

        override fun canBindData(item: Any): Boolean {
            return item is MuscleGoal
        }

        override fun bind(holder: ViewHolder, item: MuscleGoal) {
            holder.render()
        }
    }

    class ViewHolder(itemView: View, private val listener: OnListItemListener<MuscleGoal>) : ItemViewHolder<MuscleGoal>(itemView) {

        private val binding = RowSettingsMuscleGoalBinding.bind(itemView)

        fun render() {
            renderGoal()
            renderPro()
            // Clicks
            itemView.setOnClickListener { listener.onItemClicked(item, absoluteAdapterPosition) }
        }

        // Render

        private fun renderGoal() {
            binding.radioButton.isChecked = SettingsManager.manager.muscleGoal() == item
            binding.titleLbl.text = item.valueName(itemView.context)
            binding.subtitleLbl.text = item.percent1RMSummary(itemView.context)
        }

        private fun renderPro() {
            val proLocked = item.proLocked() && LocalUserManager.getUser()?.isPro() == false
            binding.proBadge.visibility = if (proLocked) View.VISIBLE else View.GONE
            binding.titleLbl.alpha = if (proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
            binding.subtitleLbl.alpha = if (proLocked) ELConstants.ALPHA_PRO_FEATURE_DISABLED else 1f
        }
    }
}