package com.everlog.data.datastores.base;

import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent;
import com.everlog.data.model.ELFirestoreModel;
import com.everlog.utils.Utils;
import com.google.firebase.firestore.SetOptions;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Keeps items in {@link InMemoryDatabase} instead of Firestore, used during Firebase Test Lab runs
 * so robots don't create data. Subclasses post the same events as the Firestore store they stand
 * in for.
 */
public abstract class InMemoryDocumentStore<T extends ELFirestoreModel> implements DocumentStore<T> {

    private InMemoryDatabase.Listener mListener;
    private volatile String mItemId;

    // Loads run one at a time and post in that order, so an older item can't replace a newer one
    private final Object mLoadLock = new Object();

    protected abstract ELDocStoreItemLoadedEvent<T> getDocumentStoreItemLoadedEvent(@Nullable T item,
                                                                                    boolean hasPendingWrites,
                                                                                    boolean fromCache,
                                                                                    @Nullable Throwable error);

    protected abstract Class<T> getType();

    /**
     * The {@link InMemoryDatabase} collection the items are kept in.
     */
    protected abstract @NonNull String getCollection();

    protected void decorateItem(T item) {
        // No-op
    }

    @Override
    public void getItem(String itemId) {
        getItem(itemId, null);
    }

    @Override
    public void getItem(String itemId, @Nullable OnStoreItemListener<T> listener) {
        // Like the Firestore store, keep sending updates for the last item loaded
        removeListener();
        mItemId = itemId;
        mListener = () -> load(itemId, null);
        InMemoryDatabase.addListener(getCollection(), mListener);
        load(itemId, listener);
    }

    @Override
    public void create(T item, SetOptions options) {
        InMemoryDatabase.put(getCollection(), item.documentId(), item);
    }

    @Override
    public void delete(T item) {
        InMemoryDatabase.delete(getCollection(), item.documentId());
    }

    @Override
    public void destroy() {
        removeListener();
        mItemId = null;
    }

    private void removeListener() {
        if (mListener != null) {
            InMemoryDatabase.removeListener(mListener);
        }
        mListener = null;
    }

    private void load(String itemId, @Nullable OnStoreItemListener<T> listener) {
        Utils.runInBackground(() -> {
            synchronized (mLoadLock) {
                T item = getType().cast(InMemoryDatabase.get(getCollection(), itemId));
                if (item != null) {
                    decorateItem(item);
                }
                Utils.runInForeground(() -> {
                    // Only the item being watched sends events, like the Firestore store
                    boolean watched = itemId != null && itemId.equals(mItemId);
                    if (item != null) {
                        if (watched) {
                            EventBus.getDefault().post(getDocumentStoreItemLoadedEvent(item, false, false, null));
                        }
                        if (listener != null) {
                            listener.onItemLoaded(item, false);
                        }
                    } else {
                        Throwable error = new ELDocumentStore.ItemNotFoundError(getType());
                        if (watched) {
                            EventBus.getDefault().post(getDocumentStoreItemLoadedEvent(null, false, false, error));
                        }
                        if (listener != null) {
                            listener.onItemLoadingError(error);
                        }
                    }
                });
            }
        });
    }
}
