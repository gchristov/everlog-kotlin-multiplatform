package com.everlog.data.datastores.base;

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

    private final String mCollection;
    private final ELCollectionStore<T> mFirestoreStore;
    private final Comparator<T> mOrder;
    private final ItemDecorator<T> mDecorator;

    private InMemoryDatabase.Listener mListener;

    /**
     * @param firestoreStore the Firestore store for the same items, for its events and decoration
     * @param order the Firestore query's ordering
     */
    public InMemoryCollectionStore(String collection, ELCollectionStore<T> firestoreStore, Comparator<T> order) {
        this(collection, firestoreStore, order, firestoreStore::decorateItem);
    }

    public InMemoryCollectionStore(String collection, ELCollectionStore<T> firestoreStore, Comparator<T> order, ItemDecorator<T> decorator) {
        mCollection = collection;
        mFirestoreStore = firestoreStore;
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
                EventBus.getDefault().post(mFirestoreStore.getCollectionStoreItemLoadedEvent(items, null, false));
                if (listener != null) {
                    listener.onItemsLoaded(items, false);
                }
            });
        });
    }
}
