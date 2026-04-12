package com.everlog.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.data.model.util.Creator;

public class CreatorAdapter {

    public static class Binder extends ItemBinder<Creator, ViewHolder> {

        private final int mLayoutResId;
        private final OnListItemListener<Creator> mListener;

        public Binder(int layoutResId, OnListItemListener<Creator> actionListener) {
            this.mLayoutResId = layoutResId;
            this.mListener = actionListener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(mLayoutResId, parent, false));
        }

        @Override
        public void bind(ViewHolder holder, Creator item) {
            holder.itemView.setOnClickListener(v -> mListener.onItemClicked(item, holder.getAbsoluteAdapterPosition()));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof Creator;
        }
    }

    static class ViewHolder extends ItemViewHolder<Creator> {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
