package com.everlog.data.datastores.events.document;

import com.everlog.data.datastores.events.BaseEvent;

public abstract class ELDocStoreItemLoadedEvent<T> extends BaseEvent {

    private T item;
    private boolean hasPendingWrites;
    private boolean fromCache;
    private Throwable error;

    public ELDocStoreItemLoadedEvent(T item,
                                     boolean hasPendingWrites,
                                     boolean fromCache,
                                     Throwable error) {
        this.item = item;
        this.hasPendingWrites = hasPendingWrites;
        this.fromCache = fromCache;
        this.error = error;
    }

    public T getItem() {
        return item;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isHasPendingWrites() {
        return hasPendingWrites;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ELDocStoreItemLoadedEvent<?> that = (ELDocStoreItemLoadedEvent<?>) o;

        if (hasPendingWrites != that.hasPendingWrites) return false;
        if (fromCache != that.fromCache) return false;
        if (item != null ? !item.equals(that.item) : that.item != null) return false;
        return error != null ? error.equals(that.error) : that.error == null;
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (hasPendingWrites ? 1 : 0);
        result = 31 * result + (fromCache ? 1 : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }
}
