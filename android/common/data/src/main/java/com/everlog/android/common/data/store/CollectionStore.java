package com.everlog.android.common.data.store;

/**
 * Loads all items of one type. Loaded items are also posted as the store's EventBus events.
 */
public interface CollectionStore<T> {

    void getItems();

    void getItems(OnStoreItemsListener<T> listener);

    void destroy();
}
