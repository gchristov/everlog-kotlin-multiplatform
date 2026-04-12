package com.everlog.ui.adapters;

public interface OnPreviousListItemListener<T> extends OnListItemListener<T> {

    T getPreviousItem(int position);
}
