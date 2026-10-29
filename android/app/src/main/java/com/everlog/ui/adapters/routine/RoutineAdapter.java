package com.everlog.ui.adapters.routine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.ELRoutine;
import com.everlog.databinding.RowRoutineBinding;
import com.everlog.ui.adapters.OnListItemListener;

public class RoutineAdapter {

    public static class Binder extends ItemBinder<ELRoutine, ViewHolder> {

        private OnRoutineListener mListener;

        public Binder(OnRoutineListener actionListener) {
            this.mListener = actionListener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_routine, parent, false));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELRoutine;
        }

        @Override
        public void bind(ViewHolder holder, ELRoutine item) {
            holder.setActionListener(mListener);
            holder.setItem(item);
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
                }
            });
        }
    }

    static class ViewHolder extends ItemViewHolder<ELRoutine> {

        private final RowRoutineBinding binding;
        private OnRoutineListener mActionListener;

        ViewHolder(View itemView) {
            super(itemView);
            binding = RowRoutineBinding.bind(itemView);
        }

        public void setActionListener(OnRoutineListener actionListener) {
            this.mActionListener = actionListener;
        }

        public void setItem(ELRoutine item) {
            binding.routineName.setText(item.getName());
        }
    }

    public interface OnRoutineListener extends OnListItemListener<ELRoutine> {}
}
