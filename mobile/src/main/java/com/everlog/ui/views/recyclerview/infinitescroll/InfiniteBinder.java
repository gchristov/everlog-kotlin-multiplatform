package com.everlog.ui.views.recyclerview.infinitescroll;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.everlog.R;

public class InfiniteBinder extends ItemBinder<Object, InfiniteViewHolder> {

    public InfiniteBinder() {
        // No-op.
    }

    @Override
    public InfiniteViewHolder create(LayoutInflater inflater, ViewGroup parent) {
        return new InfiniteViewHolder(inflater.inflate(R.layout.row_footer_infinite, parent, false));
    }

    @Override
    public boolean canBindData(Object item) {
        return item == null;
    }

    @Override
    public void bind(InfiniteViewHolder holder, Object item) {
        holder.setItem(item);
    }
}
