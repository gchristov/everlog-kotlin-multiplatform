package com.everlog.data.datastores.base;

import com.everlog.data.datastores.events.BaseEvent;
import com.everlog.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.Comparator;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Lists the items in one {@link InMemoryDatabase} collection, used during Firebase Test Lab runs
 * so robots don't create data. Posts the same events as the Firestore store it stands in for, and
 * posts them again whenever the collection changes.
 */
public class InMemoryCollectionStore<T> implements CollectionStore<T> {

    /**
     * Builds the event posted when the items are loaded, the same one the Firestore store posts.
     */
    public interface LoadedEventFactory<T> {

        BaseEvent create(@Nullable List<T> items, @Nullable Throwable error, boolean fromCache);
    }

    private final String mCollection;
    private final LoadedEventFactory<T> mLoadedEvent;
    private final Comparator<T> mOrder;
    private final ItemDecorator<T> mDecorator;

    private InMemoryDatabase.Listener mListener;

    /**
     * @param order the Firestore query's ordering
     */
    public InMemoryCollectionStore(String collection,
                                   LoadedEventFactory<T> loadedEvent,
                                   Comparator<T> order,
                                   ItemDecorator<T> decorator) {
        mCollection = collection;
        mLoadedEvent = loadedEvent;
        mOrder = order;
        mDecorator = decorator;
    }

    @Override
    public void getItems() {
        getItems(null);
    }

    @Override
    public void getItems(@Nullable OnStoreItemsListener<T> listener) {
        if (mListener == null) {
            mListener = () -> load(null);
            InMemoryDatabase.addListener(mCollection, mListener);
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
            List<T> items = InMemoryDatabase.items(mCollection, mOrder);
            for (T item : items) {
                mDecorator.decorate(item);
            }
            Utils.runInForeground(() -> {
                EventBus.getDefault().post(mLoadedEvent.create(items, null, false));
                if (listener != null) {
                    listener.onItemsLoaded(items, false);
                }
            });
        });
    }
}
