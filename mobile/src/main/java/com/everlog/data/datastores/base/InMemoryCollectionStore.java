package com.everlog.data.datastores.base;

import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent;
import com.everlog.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lists the items in one {@link InMemoryDatabase} collection, used during Firebase Test Lab runs
 * so robots don't create data. Subclasses post the same events as the Firestore store they stand
 * in for, and they're posted again whenever the collection changes.
 */
public abstract class InMemoryCollectionStore<T> implements CollectionStore<T> {

    private InMemoryDatabase.Listener mListener;

    // Loads run one at a time and post in that order, so an older list can't replace a newer one
    private final Object mLoadLock = new Object();

    protected abstract ELColStoreItemsLoadedEvent<T> getCollectionStoreItemLoadedEvent(@Nullable List<T> items,
                                                                                       @Nullable Throwable error,
                                                                                       boolean fromCache);

    /**
     * The {@link InMemoryDatabase} collection the items are kept in.
     */
    protected abstract @NonNull String getCollection();

    /**
     * The same ordering as the Firestore store's query.
     */
    protected abstract @NonNull Comparator<T> getOrder();

    protected void decorateItem(T item) {
        // No-op
    }

    @Override
    public void getItems() {
        getItems(null);
    }

    @Override
    public void getItems(@Nullable OnStoreItemsListener<T> listener) {
        if (mListener == null) {
            mListener = () -> load(null);
            InMemoryDatabase.addListener(getCollection(), mListener);
        }
        load(listener);
    }

    @Override
    public void destroy() {
        if (mListener != null) {
            InMemoryDatabase.removeListener(mListener);
        }
        mListener = null;
    }

    private void load(@Nullable OnStoreItemsListener<T> listener) {
        Utils.runInBackground(() -> {
            synchronized (mLoadLock) {
                List<T> items = InMemoryDatabase.items(getCollection(), getOrder());
                for (T item : items) {
                    decorateItem(item);
                }
                Utils.runInForeground(() -> {
                    EventBus.getDefault().post(getCollectionStoreItemLoadedEvent(items, null, false));
                    if (listener != null) {
                        listener.onItemsLoaded(items, false);
                    }
                });
            }
        });
    }
}
