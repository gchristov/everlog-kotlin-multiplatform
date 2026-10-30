package com.everlog.android.common.data.store;

public interface OnStoreItemListener<T> {

    void onItemLoaded(T item, boolean fromCache);

    void onItemLoadingError(Throwable throwable);
}
