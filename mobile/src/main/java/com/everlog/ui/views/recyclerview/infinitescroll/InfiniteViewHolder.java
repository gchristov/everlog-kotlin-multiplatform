package com.everlog.ui.views.recyclerview.infinitescroll;

import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemViewHolder;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class InfiniteViewHolder extends ItemViewHolder<Object> {

    private Object item;

    public InfiniteViewHolder(View itemView) {
        super(itemView);
    }

    public void setItem(Object item) {
        this.item = item;
        setFullSpan();
    }

    private void setFullSpan() {
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) params;
            layoutParams.setFullSpan(true);
        }
    }
}
