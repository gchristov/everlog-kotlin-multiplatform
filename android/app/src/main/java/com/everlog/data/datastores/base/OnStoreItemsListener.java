package com.everlog.data.datastores.base;

import java.util.List;

public interface OnStoreItemsListener<T> {

    void onItemsLoaded(List<T> items, boolean fromCache);

    void onItemsLoadingError(Throwable throwable);
}
