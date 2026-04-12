package com.everlog.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.databinding.RowHeaderWorkoutDetailsBinding;
import com.everlog.managers.preferences.SettingsManager;
import com.everlog.utils.DateExtKt;
import com.everlog.utils.format.StatsFormatUtils;

public class WorkoutDetailsStatsAdapter {

    public static class Binder extends ItemBinder<ELWorkout, ViewHolder> {

        private final OnWorkoutDetailsStatsListener mListener;

        public Binder(OnWorkoutDetailsStatsListener listener) {
            this.mListener = listener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_header_workout_details, parent, false), mListener);
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELWorkout;
        }

        @Override
        public void bind(ViewHolder holder, ELWorkout item) {
            holder.setItem(item);
        }
    }

    static class ViewHolder extends ItemViewHolder<ELWorkout> {

        private final RowHeaderWorkoutDetailsBinding binding;
        private final OnWorkoutDetailsStatsListener mListener;

        ViewHolder(View itemView, OnWorkoutDetailsStatsListener listener) {
            super(itemView);
            this.mListener = listener;
            binding = RowHeaderWorkoutDetailsBinding.bind(itemView);
            setupClickListeners();
        }

        private void setupClickListeners() {
            binding.nameBtn.setOnClickListener(v -> onClickEditName(getItem()));
            binding.dateBtn.setOnClickListener(v -> onClickEditDate(getItem()));
            binding.notesBtn.setOnClickListener(v -> onClickEditNotes(getItem()));
            binding.exercisesBtn.setOnClickListener(v -> onClickEditExercises(getItem()));
        }

        void onClickEditName(ELWorkout item) {
            if (mListener != null) {
                mListener.onClickEditName(item);
            }
        }

        void onClickEditDate(ELWorkout item) {
            if (mListener != null) {
                mListener.onClickEditDate(item);
            }
        }

        void onClickEditNotes(ELWorkout item) {
            if (mListener != null) {
                mListener.onClickEditNote(item);
            }
        }

        void onClickEditExercises(ELWorkout item) {
            if (mListener != null) {
                mListener.onClickEditExercises(item);
            }
        }

        public void setItem(ELWorkout item) {
            renderStats(item);
            renderWorkout(item);
        }

        // Render

        private void renderWorkout(ELWorkout item) {
            binding.workoutName.setText(item.getName());
            binding.dateLbl.setText(DateExtKt.workoutFormatted(item.getCompletedDateAsDate()));
            binding.noteLbl.setText(item.getNote());
        }

        private void renderStats(ELWorkout item) {
            // Duration
            binding.totalTimeSummary.setSummary(StatsFormatUtils.Companion.formatTimeStatsLabel(item.getDurationMillis()), getContext().getString(R.string.hour), getContext().getString(R.string.workout_details_duration));
            // Weight
            float weight = item.getTotalWeight();
            binding.totalWeightSummary.setVisibility(weight > 0 ? View.VISIBLE : View.GONE);
            binding.totalWeightSummary.setSummary(StatsFormatUtils.Companion.formatWeightStatsLabel(weight), SettingsManager.weightUnitAbbreviation(), getContext().getString(R.string.weight));
            binding.maxWeightSummary.setVisibility(weight > 0 ? View.VISIBLE : View.GONE);
            binding.maxWeightSummary.setSummary(StatsFormatUtils.Companion.formatWeightStatsLabel(item.getMaxWeight()), SettingsManager.weightUnitAbbreviation(), getContext().getString(R.string.workout_details_max_weight));
            // Exercises
            binding.exercisesSummary.setVisibility(weight > 0 ? View.GONE : View.VISIBLE);
            binding.exercisesSummary.setSummary(item.getTotalExercises() + "", null, getContext().getString(R.string.exercises_title));
            // Separators
            binding.maxWeightSeparator.setVisibility(weight > 0 ? View.VISIBLE : View.GONE);
        }

        private Context getContext() {
            return itemView.getContext();
        }
    }

    public interface OnWorkoutDetailsStatsListener extends OnListItemListener<ELWorkout> {

        void onClickEditName(ELWorkout workout);

        void onClickEditDate(ELWorkout workout);

        void onClickEditNote(ELWorkout workout);

        void onClickEditExercises(ELWorkout workout);
    }
}
