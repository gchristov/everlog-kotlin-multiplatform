package com.everlog.android.common.mvp.list;

public interface OnListItemListener<T> {

    void onItemClicked(T item, int position);
}
