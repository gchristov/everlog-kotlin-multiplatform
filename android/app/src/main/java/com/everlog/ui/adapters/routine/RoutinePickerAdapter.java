package com.everlog.ui.adapters.routine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.ELRoutine;
import com.everlog.databinding.RowRoutinePickerBinding;
import com.everlog.ui.adapters.OnListItemListener;

public class RoutinePickerAdapter {

    public static class Binder extends ItemBinder<ELRoutine, ViewHolder> {

        private final OnListItemListener<ELRoutine> mListener;

        public Binder(OnListItemListener<ELRoutine> actionListener) {
            this.mListener = actionListener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_routine_picker, parent, false));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELRoutine;
        }

        @Override
        public void bind(ViewHolder holder, ELRoutine item) {
            holder.setItem(item);
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
                }
            });
        }
    }

    static class ViewHolder extends ItemViewHolder<ELRoutine> {

        private final RowRoutinePickerBinding binding;

        ViewHolder(View itemView) {
            super(itemView);
            binding = RowRoutinePickerBinding.bind(itemView);
        }

        public void setItem(ELRoutine item) {
            renderSummary(item);
            renderPickerMode();
        }

        private void renderPickerMode() {
            binding.rightArrow.setVisibility(View.VISIBLE);
        }

        private void renderSummary(ELRoutine item) {
            binding.routineName.setText(item.getName());
            binding.routineSummary.setText(item.getSummary());
        }
    }
}
