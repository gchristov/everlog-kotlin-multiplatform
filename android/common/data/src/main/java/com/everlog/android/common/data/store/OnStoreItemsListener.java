package com.everlog.android.common.data.store;

import java.util.List;

public interface OnStoreItemsListener<T> {

    void onItemsLoaded(List<T> items, boolean fromCache);

    void onItemsLoadingError(Throwable throwable);
}
