package com.everlog.data.datastores.base;

import com.google.firebase.firestore.SetOptions;

/**
 * Loads, saves and deletes single items of one type. Loaded items are also posted as the store's
 * EventBus events.
 */
public interface DocumentStore<T> {

    void getItem(String itemId);

    void getItem(String itemId, OnStoreItemListener<T> listener);

    void create(T item, SetOptions options);

    void delete(T item);

    void destroy();
}
