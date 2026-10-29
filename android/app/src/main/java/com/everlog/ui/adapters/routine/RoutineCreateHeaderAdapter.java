package com.everlog.ui.adapters.routine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.ELRoutine;
import com.everlog.databinding.RowHeaderRoutineCreateBinding;
import com.everlog.ui.adapters.OnListItemListener;

public class RoutineCreateHeaderAdapter {

    public static class Binder extends ItemBinder<ELRoutine, ViewHolder> {

        private final OnListItemListener<ELRoutine> mListener;

        public Binder(OnListItemListener<ELRoutine> listener) {
            this.mListener = listener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_header_routine_create, parent, false), mListener);
        }

        @Override
        public void bind(ViewHolder holder, ELRoutine item) {
            holder.render(item);
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELRoutine;
        }
    }

    static class ViewHolder extends ItemViewHolder<ELRoutine> {

        private final RowHeaderRoutineCreateBinding binding;
        private final OnListItemListener<ELRoutine> mListener;

        ViewHolder(View itemView, OnListItemListener<ELRoutine> listener) {
            super(itemView);
            this.mListener = listener;
            binding = RowHeaderRoutineCreateBinding.bind(itemView);
        }

        // Render

        protected void render(ELRoutine item) {
            binding.nameField.setText(item.getName());
            // Clicks
            binding.nameBtn.setOnClickListener(view -> {
                mListener.onItemClicked(item, getAbsoluteAdapterPosition());
            });
        }
    }
}
