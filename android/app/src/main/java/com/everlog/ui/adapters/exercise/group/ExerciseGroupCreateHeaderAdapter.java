package com.everlog.ui.adapters.exercise.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.util.ExerciseGroupCreateHeader;
import com.everlog.databinding.RowHeaderExerciseGroupCreateBinding;
import com.everlog.ui.adapters.OnListItemListener;
import com.everlog.utils.format.FormatUtils;

public class ExerciseGroupCreateHeaderAdapter {

    public static class Binder extends ItemBinder<ExerciseGroupCreateHeader, ViewHolder> {

        private final OnExerciseGroupCreateHeaderListener mListener;

        public Binder(OnExerciseGroupCreateHeaderListener listener) {
            this.mListener = listener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_header_exercise_group_create, parent, false), mListener);
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ExerciseGroupCreateHeader;
        }

        @Override
        public void bind(ViewHolder holder, ExerciseGroupCreateHeader item) {
            holder.render(item);
        }
    }

    static class ViewHolder extends ItemViewHolder<ExerciseGroupCreateHeader> {

        private final RowHeaderExerciseGroupCreateBinding binding;
        private final OnExerciseGroupCreateHeaderListener mListener;

        ViewHolder(View itemView, OnExerciseGroupCreateHeaderListener listener) {
            super(itemView);
            this.mListener = listener;
            binding = RowHeaderExerciseGroupCreateBinding.bind(itemView);
        }

        // Render

        protected void render(ExerciseGroupCreateHeader item) {
            binding.restTimeLbl.setText(FormatUtils.formatRestTime(item.getRestTimeSeconds()));
            binding.exerciseTimeLbl.setText(FormatUtils.formatExerciseTime(item.getExerciseTimeSeconds(), item.getExerciseTimeMixed()));
            // Clicks
            binding.restTimeBtn.setOnClickListener(view -> {
                mListener.onRestTimeClick();
            });
            binding.exerciseTimeBtn.setOnClickListener(view -> {
                mListener.onExerciseTimeClick();
            });
        }
    }

    public interface OnExerciseGroupCreateHeaderListener extends OnListItemListener<ExerciseGroupCreateHeader> {
        void onRestTimeClick();
        void onExerciseTimeClick();
    }
}
