package com.everlog.ui.adapters.exercise;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.data.model.exercise.ExerciseSuggestion;
import com.everlog.databinding.RowExerciseBinding;
import com.everlog.ui.adapters.OnListItemListener;

import androidx.core.content.ContextCompat;

public class ExerciseAdapter {

    public static class Binder extends ItemBinder<ELExercise, ViewHolder> {

        private final boolean mSelectionMode;
        private final OnExerciseListener listener;

        public Binder(boolean selectionMode, OnExerciseListener actionListener) {
            this.listener = actionListener;
            this.mSelectionMode = selectionMode;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_exercise, parent, false),
                    listener,
                    mSelectionMode);
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELExercise;
        }

        @Override
        public void bind(ViewHolder holder, ELExercise item) {
            holder.setItem(item);
            holder.itemView.setOnClickListener(v -> {
                listener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
            });
        }
    }

    public static class SuggestionBinder extends ItemBinder<ExerciseSuggestion, SuggestionViewHolder> {

        private final OnListItemListener<ExerciseSuggestion> listener;

        public SuggestionBinder(OnListItemListener<ExerciseSuggestion> listener) {
            this.listener = listener;
        }

        @Override
        public SuggestionViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new SuggestionViewHolder(inflater.inflate(R.layout.row_exercise, parent, false), listener);
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ExerciseSuggestion;
        }

        @Override
        public void bind(SuggestionViewHolder holder, ExerciseSuggestion item) {
            holder.setItem(item);
            holder.itemView.setOnClickListener(v -> {
                listener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
            });
        }
    }

    static class ViewHolder extends ItemViewHolder<ELExercise> {

        private final RowExerciseBinding binding;
        private final boolean mSelectionMode;
        private final OnExerciseListener mActionListener;

        ViewHolder(View itemView,
                   OnExerciseListener actionListener,
                   boolean selectionMode) {
            super(itemView);
            binding = RowExerciseBinding.bind(itemView);
            this.mSelectionMode = selectionMode;
            this.mActionListener = actionListener;
        }

        public void setItem(ELExercise item) {
            renderExercise(item);
            renderSectionTitle(item);
            renderSelection(item);
        }

        // Render

        private void renderSelection(ELExercise item) {
            binding.selectedCheckbox.setVisibility(mSelectionMode ? View.VISIBLE : View.GONE);
            binding.selectedCheckbox.setChecked(mActionListener.isSelected(item));
        }

        private void renderExercise(ELExercise item) {
            binding.exerciseImg.setExercise(item);
            binding.exerciseName.setText(item.getName());
            binding.categoryName.setText(item.getCategory());
        }

        private void renderSectionTitle(ELExercise item) {
            binding.sectionTitle.setText(item.getFirstChar());
            ELExercise previous = mActionListener.getPreviousItem(getAbsoluteAdapterPosition());
            binding.sectionTitle.setVisibility(firstCharMatchesExercise(item, previous) ? View.GONE : View.VISIBLE);
        }

        // Utils

        private boolean firstCharMatchesExercise(ELExercise item, ELExercise exercise) {
            if (exercise == null) {
                return false;
            }
            return item.getFirstChar().equals(exercise.getFirstChar());
        }
    }

    static class SuggestionViewHolder extends ItemViewHolder<ExerciseSuggestion> {

        private final RowExerciseBinding binding;
        private final OnListItemListener<ExerciseSuggestion> listener;

        SuggestionViewHolder(View itemView, OnListItemListener<ExerciseSuggestion> listener) {
            super(itemView);
            binding = RowExerciseBinding.bind(itemView);
            this.listener = listener;
        }

        public void setItem(ExerciseSuggestion item) {
            renderExercise(item);
            renderSectionTitle(item);
        }

        // Render

        private void renderExercise(ExerciseSuggestion item) {
            binding.exerciseImg.setExercise(item);
            binding.exerciseName.setText(item.getName());
            binding.categoryName.setText(R.string.exercises_create_prompt);
            binding.categoryName.setBackground(null);
            binding.categoryName.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_1));
            binding.categoryName.setAllCaps(false);
            binding.categoryName.setPadding(0, 0, 0, 0);
            binding.categoryName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }

        private void renderSectionTitle(ExerciseSuggestion item) {
            binding.sectionTitle.setText(item.getFirstChar());
            binding.sectionTitle.setVisibility(View.VISIBLE);
        }
    }

    public interface OnExerciseListener extends OnListItemListener<ELExercise> {

        ELExercise getPreviousItem(int position);

        boolean isSelected(ELExercise exercise);
    }
}
