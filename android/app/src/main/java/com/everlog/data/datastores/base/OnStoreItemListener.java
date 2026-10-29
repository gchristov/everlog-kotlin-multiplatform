package com.everlog.data.datastores.base;

public interface OnStoreItemListener<T> {

    void onItemLoaded(T item, boolean fromCache);

    void onItemLoadingError(Throwable throwable);
}
