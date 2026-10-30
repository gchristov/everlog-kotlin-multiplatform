package com.everlog.ui.adapters;

import com.everlog.android.common.mvp.list.OnListItemListener;

public interface OnPreviousListItemListener<T> extends OnListItemListener<T> {

    T getPreviousItem(int position);
}
