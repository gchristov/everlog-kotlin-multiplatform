package com.everlog.data.datastores.base;

import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent;
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent;
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent;
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent;
import com.everlog.utils.Utils;
import com.everlog.utils.device.DeviceUtils;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public abstract class ELCollectionStore<T> {

    private final List<T> mItems = Collections.synchronizedList(new ArrayList<>());
    // What Firestore returned during Firebase Test Lab runs, where mItems also has what the run
    // wrote in memory. Unused otherwise.
    private final List<T> mServerItems = Collections.synchronizedList(new ArrayList<>());

    // Callbacks

    private final PublishSubject<List<T>> mItemsLoadedObservable = PublishSubject.create();
    private final List<OnStoreItemsListener<T>> mOneTimeListeners = Collections.synchronizedList(new ArrayList<>());

    // Firebase

    private ListenerRegistration mRegistration;

    // Firebase Test Lab

    private TestLabStore.Listener mTestLabListener;

    // Snapshots

    private boolean mSnapshotAdded;
    private boolean mCacheLoadedOnce;
    private boolean mItemsLoadedOnce;
    
    // Synchronization
    
    private final Semaphore mLoadMutex = new Semaphore(1);
    private final Object mParseMutex = new Object();

    protected abstract ELColStoreItemAddedEvent<T> getCollectionStoreItemAddedEvent(int position,
                                                                                    T item,
                                                                                    boolean hasPendingWrites,
                                                                                    boolean fromCache);

    protected abstract ELColStoreItemModifiedEvent<T> getCollectionStoreItemModifiedEvent(int oldPosition,
                                                                                          int newPosition,
                                                                                          T item,
                                                                                          boolean hasPendingWrites,
                                                                                          boolean fromCache);

    protected abstract ELColStoreItemRemovedEvent<T> getCollectionStoreItemRemovedEvent(int position,
                                                                                        T item,
                                                                                        boolean hasPendingWrites,
                                                                                        boolean fromCache);

    protected abstract ELColStoreItemsLoadedEvent<T> getCollectionStoreItemLoadedEvent(@Nullable List<T> items,
                                                                                       Throwable error,
                                                                                       boolean fromCache);

    protected abstract Class<T> getType();

    protected abstract @NonNull Query getQuery();

    protected abstract String getTag();

    /**
     * The collection the query reads, so that what a Firebase Test Lab run writes to it in memory
     * shows up in this store. Null leaves out in-memory writes.
     */
    protected @Nullable String getTestLabCollectionPath() {
        return null;
    }

    /**
     * How the query orders items, for placing in-memory writes during Firebase Test Lab runs. Null
     * adds them at the end.
     */
    protected @Nullable Comparator<T> getTestLabOrder() {
        return null;
    }

    public PublishSubject<List<T>> onItemsLoadedSubscription() {
        return mItemsLoadedObservable;
    }

    public void destroy() {
        Timber.tag(getTag()).d("Destroying");
        removeSnapshotListener();
        removeTestLabListener();
        mSnapshotAdded = false;
        mCacheLoadedOnce = false;
        mItemsLoadedOnce = false;
        mItems.clear();
        mServerItems.clear();
        mOneTimeListeners.clear();
        releaseLoadMutex();
    }

    public void getItems() {
        getItems(null, false);
    }

    public void getItems(OnStoreItemsListener<T> listener) {
        getItems(listener, true);
    }

    // Fetch

    private void getItems(OnStoreItemsListener<T> listener, boolean oneTime) {
        Utils.runInBackground(() -> doGetItems(listener, oneTime));
    }

    private void doGetItems(OnStoreItemsListener<T> oneTimeListener, boolean oneTime) {
        try {
            // Acquire lock
            Timber.tag(getTag()).d("Acquiring lock");
            Timber.tag(getTag()).d("Available Semaphore permits: %s", mLoadMutex.availablePermits());
            mLoadMutex.acquire();
            Timber.tag(getTag()).d("Acquired lock");

            if (oneTime) {
                safelyAdd(mOneTimeListeners, oneTimeListener);
            }
            if (mSnapshotAdded) {
                // Notify listeners immediately
                if (oneTime) {
                    Timber.tag(getTag()).d("Items loaded once. Replying to on-time listeners: items=%s", mItems.size());
                    oneTimeListener.onItemsLoaded(mItems, false);
                    mOneTimeListeners.remove(oneTimeListener);
                } else {
                    Timber.tag(getTag()).d("Items loaded once. Broadcasting to subscribers: items=%s", mItems.size());
                    sendItemsLoadedEvent(false);
                    mItemsLoadedObservable.onNext(new ArrayList<>(mItems));
                }
                releaseLoadMutex();
            } else {
                addSnapshotListener();
            }
        } catch (InterruptedException e) {
            notifyError(false, e);
        }
    }

    // Firebase

    private void removeSnapshotListener() {
        if (mRegistration != null) {
            mRegistration.remove();
        }
        mRegistration = null;
    }

    private void removeTestLabListener() {
        if (mTestLabListener != null) {
            TestLabStore.removeListener(mTestLabListener);
        }
        mTestLabListener = null;
    }

    private @Nullable String testLabCollectionPath() {
        return DeviceUtils.isFirebaseTestLabRun() ? getTestLabCollectionPath() : null;
    }

    private void listenToTestLabStore() {
        removeTestLabListener();
        String path = testLabCollectionPath();
        if (path == null) {
            return;
        }
        mTestLabListener = () -> Utils.runInBackground(() -> {
            synchronized (mParseMutex) {
                if (mItemsLoadedOnce) {
                    publishItems();
                    notifyItemsChanged();
                }
            }
        });
        TestLabStore.addListener(path, mTestLabListener);
    }

    private void addSnapshotListener() {
        Timber.tag(getTag()).d("Adding snapshot listener");
        removeSnapshotListener();
        listenToTestLabStore();
        Query query = getQuery();
        // Try resolving the required data from cache first
        query.get(Source.CACHE).addOnCompleteListener(result -> {
            if (!mCacheLoadedOnce) {
                Timber.tag(getTag()).d("Cleared items because cache wasn't loaded once");
                mItems.clear();
                mServerItems.clear();
            }
            mCacheLoadedOnce = true;
            // Wait until cache is parsed first
            OnCacheParsedListener cacheFinishedListener = () -> {
                // Continue with snapshot
                mRegistration = query
                        .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
                            if (e != null || snapshot == null) {
                                notifyError(false, e != null ? e : new RuntimeException("Snapshot is invalid"));
                            } else {
                                if (!mSnapshotAdded) {
                                    Timber.tag(getTag()).d("Cleared items because snapshot wasn't loaded once");
                                    mSnapshotAdded = true;
                                    mItems.clear();
                                    mServerItems.clear();
                                }
                                Timber.tag(getTag()).d("Received items from snapshot: items=%s fromCache=%s", snapshot.getDocuments().size(), false);
                                enqueueSnapshot(snapshot, false, null);
                            }
                        });
            };
            if (result.isSuccessful()) {
                QuerySnapshot snapshot = result.getResult();
                Timber.tag(getTag()).d("Received items from cache: items=%s", snapshot.getDocuments().size());
                // Parse cache if we have something
                enqueueSnapshot(snapshot, true, cacheFinishedListener);
            } else {
                Timber.tag(getTag()).d("Failed to receive items from cache");
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
                publishItems();
                mItemsLoadedOnce = true;
                notifySnapshotReady(fromCache);
                if (cacheListener != null) {
                    cacheListener.onComplete();
                }
            }
        });
    }

    private void parseSnapshot(QuerySnapshot snapshot, boolean fromCache) {
        Timber.tag(getTag()).d("Parsing snapshot: changes=%s fromCache=%s", snapshot.getDocumentChanges().size(), fromCache);
        // Firestore's positions don't account for in-memory writes, so only the full list is sent
        boolean testLab = testLabCollectionPath() != null;
        List<T> items = testLab ? mServerItems : mItems;
        boolean notifyChanges = mItemsLoadedOnce && !testLab;
        for (DocumentChange documentChange : snapshot.getDocumentChanges()) {
            DocumentSnapshot document = documentChange.getDocument();
            if (document.exists()) {
                // Check only valid documents
                try {
                    int newIndex = documentChange.getNewIndex();
                    int oldIndex = documentChange.getOldIndex();
                    boolean hasPendingWrites = document.getMetadata().hasPendingWrites();
                    // Parse and decorate item
                    T parsedItem = document.toObject(getType());
                    decorateItem(parsedItem);
                    switch (documentChange.getType()) {
                        case ADDED: {
                            items.add(newIndex, parsedItem);
                            if (notifyChanges) {
                                notifyItemAdded(newIndex, parsedItem, hasPendingWrites, fromCache);
                            }
                            break;
                        }
                        case MODIFIED: {
                            items.remove(oldIndex);
                            items.add(newIndex, parsedItem);
                            if (notifyChanges) {
                                notifyItemModified(oldIndex, newIndex, parsedItem, hasPendingWrites, fromCache);
                            }
                            break;
                        }
                        case REMOVED: {
                            items.remove(oldIndex);
                            if (notifyChanges) {
                                notifyItemRemoved(oldIndex, parsedItem, hasPendingWrites, fromCache);
                            }
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Timber.tag(getTag()).e(ex);
                }
            }
        }
    }

    private void publishItems() {
        String path = testLabCollectionPath();
        if (path == null) {
            // mItems is already up to date
            return;
        }
        List<T> serverItems;
        synchronized (mServerItems) {
            serverItems = new ArrayList<>(mServerItems);
        }
        Set<T> decorated = Collections.newSetFromMap(new IdentityHashMap<>());
        decorated.addAll(serverItems);
        List<T> merged = TestLabStore.merge(path, serverItems, getTestLabOrder());
        for (T item : merged) {
            if (!decorated.contains(item)) {
                // In-memory copies aren't decorated yet
                decorateItem(item);
            }
        }
        synchronized (mItems) {
            mItems.clear();
            mItems.addAll(merged);
        }
    }

    protected void decorateItem(T item) {
        // No-op
    }

    // Notifications

    private void notifyError(boolean fromCache, Throwable throwable) {
        throwable.printStackTrace();
        Timber.tag(getTag()).e(throwable);
        Utils.runInForeground(() -> {
            sendItemsLoadedErrorEvent(throwable, fromCache);
            for (OnStoreItemsListener<T> listener : mOneTimeListeners) {
                listener.onItemsLoadingError(throwable);
            }
            mOneTimeListeners.clear();
            // Keep threads waiting until cache refreshes
            if (!fromCache) {
                releaseLoadMutex();
            }
        });
    }

    private void notifySnapshotReady(boolean fromCache) {
        Timber.tag(getTag()).d("Notifying snapshot ready: items=%s fromCache=%s", mItems.size(), fromCache);
        Utils.runInForeground(() -> {
            sendItemsLoadedEvent(fromCache);
            for (OnStoreItemsListener<T> listener : mOneTimeListeners) {
                listener.onItemsLoaded(mItems, fromCache);
            }
            mOneTimeListeners.clear();
            mItemsLoadedObservable.onNext(new ArrayList<>(mItems));
            // Keep threads waiting until cache refreshes
            if (!fromCache) {
                releaseLoadMutex();
            }
        });
    }

    private void notifyItemsChanged() {
        Utils.runInForeground(() -> {
            sendItemsLoadedEvent(false);
            mItemsLoadedObservable.onNext(new ArrayList<>(mItems));
        });
    }

    private void notifyItemAdded(int position,
                                 T item,
                                 boolean hasPendingWrites,
                                 boolean fromCache) {
        sendItemAddedEvent(position, item, hasPendingWrites, fromCache);
    }

    private void notifyItemModified(int oldPosition,
                                    int newPosition,
                                    T item,
                                    boolean hasPendingWrites,
                                    boolean fromCache) {
        sendItemModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache);
    }

    private void notifyItemRemoved(int position,
                                   T item,
                                   boolean hasPendingWrites,
                                   boolean fromCache) {
        sendItemRemovedEvent(position, item, hasPendingWrites, fromCache);
    }

    // Events

    private void sendItemsLoadedEvent(boolean fromCache) {
        EventBus.getDefault().post(getCollectionStoreItemLoadedEvent(mItems, null, fromCache));
    }

    private void sendItemsLoadedErrorEvent(Throwable error, boolean fromCache) {
        EventBus.getDefault().post(getCollectionStoreItemLoadedEvent(null, error, fromCache));
    }

    private void sendItemAddedEvent(int position,
                                    T item,
                                    boolean hasPendingWrites,
                                    boolean fromCache) {
        EventBus.getDefault().post(getCollectionStoreItemAddedEvent(position, item, hasPendingWrites, fromCache));
    }

    private void sendItemModifiedEvent(int oldPosition,
                                       int newPosition,
                                       T item,
                                       boolean hasPendingWrites,
                                       boolean fromCache) {
        EventBus.getDefault().post(getCollectionStoreItemModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache));
    }

    private void sendItemRemovedEvent(int position,
                                      T item,
                                      boolean hasPendingWrites,
                                      boolean fromCache) {
        EventBus.getDefault().post(getCollectionStoreItemRemovedEvent(position, item, hasPendingWrites, fromCache));
    }

    // Utils

    private void safelyAdd(Collection<OnStoreItemsListener<T>> collection, OnStoreItemsListener<T> item) {
        if (item != null && !collection.contains(item)) {
            collection.add(item);
        }
    }

    private interface OnCacheParsedListener {

         void onComplete();
    }
}
