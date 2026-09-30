package com.everlog.data.datastores.base;

import com.everlog.data.model.ELFirestoreModel;
import com.everlog.utils.Utils;
import com.google.firebase.firestore.SetOptions;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.Nullable;

/**
 * Keeps items in {@link InMemoryDatabase} instead of Firestore, used during Firebase Test Lab runs
 * so robots don't create data. Posts the same events as the Firestore store it stands in for.
 */
public class InMemoryDocumentStore<T extends ELFirestoreModel> implements DocumentStore<T> {

    private final String mCollection;
    private final ELDocumentStore<T> mFirestoreStore;
    private final ItemDecorator<T> mDecorator;

    private InMemoryDatabase.Listener mListener;

    /**
     * @param firestoreStore the Firestore store for the same items, for its events and decoration
     */
    public InMemoryDocumentStore(String collection, ELDocumentStore<T> firestoreStore) {
        this(collection, firestoreStore, firestoreStore::decorateItem);
    }

    public InMemoryDocumentStore(String collection, ELDocumentStore<T> firestoreStore, ItemDecorator<T> decorator) {
        mCollection = collection;
        mFirestoreStore = firestoreStore;
        mDecorator = decorator;
    }

    @Override
    public void getItem(String itemId) {
        getItem(itemId, null);
    }

    @Override
    public void getItem(String itemId, @Nullable OnStoreItemListener<T> listener) {
        // Like the Firestore store, keep sending updates for the last item loaded
        removeListener();
        mListener = () -> load(itemId, null);
        InMemoryDatabase.addListener(mCollection, mListener);
        load(itemId, listener);
    }

    @Override
    public void create(T item, SetOptions options) {
        InMemoryDatabase.put(mCollection, item.documentId(), item);
    }

    @Override
    public void delete(T item) {
        InMemoryDatabase.delete(mCollection, item.documentId());
    }

    @Override
    public void destroy() {
        removeListener();
    }

    private void removeListener() {
        if (mListener != null) {
            InMemoryDatabase.removeListener(mListener);
        }
        mListener = null;
    }

    private void load(String itemId, @Nullable OnStoreItemListener<T> listener) {
        Utils.runInBackground(() -> {
            T item = mFirestoreStore.getType().cast(InMemoryDatabase.get(mCollection, itemId));
            if (item != null) {
                mDecorator.decorate(item);
                mFirestoreStore.itemReady(item);
            }
            Utils.runInForeground(() -> {
                if (item != null) {
                    EventBus.getDefault().post(mFirestoreStore.getDocumentStoreItemLoadedEvent(item, false, false, null));
                    if (listener != null) {
                        listener.onItemLoaded(item, false);
                    }
                } else {
                    Throwable error = new ELDocumentStore.ItemNotFoundError(mFirestoreStore.getType());
                    EventBus.getDefault().post(mFirestoreStore.getDocumentStoreItemLoadedEvent(null, false, false, error));
                    if (listener != null) {
                        listener.onItemLoadingError(error);
                    }
                }
            });
        });
    }
}
