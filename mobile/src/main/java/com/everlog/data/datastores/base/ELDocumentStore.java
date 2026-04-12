package com.everlog.data.datastores.base;

import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent;
import com.everlog.data.model.ELFirestoreModel;
import com.everlog.managers.auth.AuthManager;
import com.everlog.utils.Utils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public abstract class ELDocumentStore<T extends ELFirestoreModel> {

    // Callbacks

    private final List<OnStoreItemListener<T>> mOneTimeListeners = Collections.synchronizedList(new ArrayList<>());

    // Firebase

    private ListenerRegistration mRegistration;

    // Snapshots

    private final Semaphore mLoadMutex = new Semaphore(1);
    private final Object mParseMutex = new Object();

    protected abstract ELDocStoreItemLoadedEvent<T> getDocumentStoreItemLoadedEvent(@Nullable T item,
                                                                                    boolean hasPendingWrites,
                                                                                    boolean fromCache,
                                                                                    @Nullable Throwable error);

    protected abstract Class<T> getType();

    protected abstract @NonNull Query getQuery(String itemId);

    protected abstract @NonNull CollectionReference getParentCollection();

    protected abstract String getTag();

    public void destroy() {
        Timber.tag(getTag()).d("Destroying");
        mOneTimeListeners.clear();
        removeSnapshotListener();
        releaseLoadMutex();
    }

    public void getItem(String itemId) {
        getItem(itemId, null);
    }

    public void getItem(String itemId, OnStoreItemListener<T> listener) {
        Utils.runInBackground(() -> doGetItem(itemId, listener));
    }

    private void doGetItem(String itemId, OnStoreItemListener<T> listener) {
        try {
            // Acquire lock
            Timber.tag(getTag()).d("Acquiring lock");
            Timber.tag(getTag()).d("Available Semaphore permits: %s", mLoadMutex.availablePermits());
            mLoadMutex.acquire();
            Timber.tag(getTag()).d("Acquired lock");

            safelyAdd(mOneTimeListeners, listener);
            addSnapshotListener(itemId);
        } catch (InterruptedException e) {
            notifyError(false, e);
        }
    }

    // CRUD

    public void create(T item, SetOptions options) {
        if (AuthManager.isRunningInGoogleTestLab()) {
            Timber.tag(getTag()).w("Ignoring CREATE operation for Google Cloud account");
            return;
        }
        if (options != null) {
            getParentCollection().document(item.documentId()).set(item.asMap(), options);
        } else {
            getParentCollection().document(item.documentId()).set(item.asMap());
        }
    }

    public void delete(T item) {
        if (AuthManager.isRunningInGoogleTestLab()) {
            Timber.tag(getTag()).w("Ignoring DELETE operation for Google Cloud account");
            return;
        }
        getParentCollection().document(item.documentId()).delete();
    }

    // Firebase

    private void removeSnapshotListener() {
        if (mRegistration != null) {
            mRegistration.remove();
        }
        mRegistration = null;
    }

    private void addSnapshotListener(String itemId) {
        Timber.tag(getTag()).d("Adding snapshot listener");
        removeSnapshotListener();
        Query query = getQuery(itemId);
        // Try resolving the required data from cache first
        query.get(Source.CACHE).addOnCompleteListener(result -> {
            // Wait until cache is parsed first
            OnCacheParsedListener cacheFinishedListener = () -> {
                // Continue with snapshot
                mRegistration = query
                        .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
                            if (e != null || snapshot == null) {
                                notifyError(false, e != null ? e : new RuntimeException("Snapshot is invalid"));
                            } else {
                                Timber.tag(getTag()).d("Received item from snapshot: item=%s fromCache=%s", snapshot.getDocuments().size(), false);
                                enqueueSnapshot(snapshot, false, null);
                            }
                        });
            };
            if (result.isSuccessful()) {
                Timber.tag(getTag()).d("Received item from cache");
                // Parse cache if we have something
                enqueueSnapshot(result.getResult(), true, cacheFinishedListener);
            } else {
                Timber.tag(getTag()).d("Failed to receive item from cache");
                cacheFinishedListener.onComplete();
            }
        });
    }

    // Threading

    private void releaseLoadMutex() {
        Timber.tag(getTag()).d("Releasing lock");
        while (mLoadMutex.availablePermits() < 1) {
            mLoadMutex.release();
        }
        Timber.tag(getTag()).d("Available Semaphore permits after release: %s", mLoadMutex.availablePermits());
    }

    // Parsing and decorating

    private void enqueueSnapshot(QuerySnapshot snapshot,
                                 boolean fromCache,
                                 OnCacheParsedListener cacheListener) {
        Utils.runInBackground(() -> {
            // Parse snapshots one by one in a queue
            synchronized (mParseMutex) {
                parseSnapshot(snapshot, fromCache);
                if (cacheListener != null) {
                    cacheListener.onComplete();
                }
            }
        });
    }

    private void parseSnapshot(QuerySnapshot snapshot, boolean fromCache) {
        Timber.tag(getTag()).d("Parsing snapshot: changes=%s fromCache=%s", snapshot.getDocumentChanges().size(), fromCache);
        try {
            // We are expecting a single result here, so only fetch the first in the list
            DocumentSnapshot document = snapshot.getDocuments().size() > 0 ? snapshot.getDocuments().get(0) : null;
            if (document == null || !document.exists()) {
                if (!fromCache) {
                    // Only notify of errors if load is not from cache
                    notifyError(false, new ItemNotFoundError(getType()));
                }
            } else {
                // Parse and decorate item
                T parsedItem = document.toObject(getType());
                decorateItem(parsedItem);
                itemReady(parsedItem);
                notifySnapshotReady(parsedItem, snapshot.getMetadata().hasPendingWrites(), fromCache);
            }
        } catch (Exception e) {
            notifyError(fromCache, e);
        }
    }

    protected void decorateItem(T item) {
        // No-op
    }

    protected void itemReady(T item) {
        // No-op
    }

    // Notifications

    private void notifyError(boolean fromCache, Throwable throwable) {
        throwable.printStackTrace();
        Timber.tag(getTag()).e(throwable);
        Utils.runInForeground(() -> {
            sendItemLoadedErrorEvent(fromCache, throwable);
            for (OnStoreItemListener<T> listener : mOneTimeListeners) {
                listener.onItemLoadingError(throwable);
            }
            mOneTimeListeners.clear();
            // Keep threads waiting until cache refreshes
            if (!fromCache) {
                releaseLoadMutex();
            }
        });
    }

    private void notifySnapshotReady(T item,
                                     boolean hasPendingWrites,
                                     boolean fromCache) {
        Timber.tag(getTag()).d("Notifying snapshot ready: item=%s hasPendingWrites=%s fromCache=%s", item != null, hasPendingWrites, fromCache);
        Utils.runInForeground(() -> {
            sendItemLoadedEvent(item, hasPendingWrites, fromCache);
            for (OnStoreItemListener<T> listener : mOneTimeListeners) {
                listener.onItemLoaded(item, fromCache);
            }
            mOneTimeListeners.clear();
            // Keep threads waiting until cache refreshes
            if (!fromCache) {
                releaseLoadMutex();
            }
        });
    }

    // Events

    private void sendItemLoadedEvent(T item,
                                     boolean hasPendingWrites,
                                     boolean fromCache) {
        EventBus.getDefault().post(getDocumentStoreItemLoadedEvent(item, hasPendingWrites, fromCache, null));
    }

    private void sendItemLoadedErrorEvent(boolean fromCache, Throwable error) {
        EventBus.getDefault().post(getDocumentStoreItemLoadedEvent(null, false, fromCache, error));
    }

    // Utils

    private void safelyAdd(Collection<OnStoreItemListener<T>> collection, OnStoreItemListener<T> item) {
        if (item != null && !collection.contains(item)) {
            collection.add(item);
        }
    }

    private interface OnCacheParsedListener {

        void onComplete();
    }

    public static class ItemNotFoundError extends Exception {
        ItemNotFoundError(Class clazz) {
            super(String.format("Item not found: type=%s", clazz));
        }
    }
}
