package com.everlog.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.set.ELSetType;
import com.everlog.ui.views.SetTypeSummaryView;

public class SetTypeAdapter {

    public static class Binder extends ItemBinder<ELSetType, ViewHolder> {

        private final OnListItemListener<ELSetType> mListener;
        private int mSelectedExercisesCount;

        public Binder(OnListItemListener<ELSetType> actionListener) {
            this.mListener = actionListener;
        }

        public void setSelectedExercisesCount(int count) {
            this.mSelectedExercisesCount = count;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_set_type, parent, false));
        }

        @Override
        public void bind(ViewHolder holder, ELSetType item) {
            holder.setItem(item, mSelectedExercisesCount);
            holder.itemView.setOnClickListener(v -> mListener.onItemClicked(item, holder.getAbsoluteAdapterPosition()));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof ELSetType;
        }
    }

    static class ViewHolder extends ItemViewHolder<ELSetType> {

        SetTypeSummaryView mSetTypeView;

        private int mSelectedExercisesCount;
        private ELSetType item;

        ViewHolder(View itemView) {
            super(itemView);
            mSetTypeView = itemView.findViewById(R.id.setTypeView);
        }

        public void setItem(ELSetType item, int selectedExercisesCount) {
            this.item = item;
            this.mSelectedExercisesCount = selectedExercisesCount;
            renderSet();
            renderSelection();
        }

        // Render

        private void renderSet() {
            mSetTypeView.setSetType(item);
        }

        private void renderSelection() {
            itemView.setAlpha(item.canBeSelected(mSelectedExercisesCount) ? 1f : 0.4f);
        }
    }
}
