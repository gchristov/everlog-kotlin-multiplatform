package com.everlog.data.datastores.events.collection;

import com.everlog.data.datastores.events.BaseEvent;

public class ELColStoreItemAddedEvent<T> extends BaseEvent {

    private int position;
    private T item;
    private boolean hasPendingWrites;
    private boolean fromCache;

    public ELColStoreItemAddedEvent(int position,
                                    T item,
                                    boolean hasPendingWrites,
                                    boolean fromCache) {
        this.position = position;
        this.item = item;
        this.hasPendingWrites = hasPendingWrites;
        this.fromCache = fromCache;
    }

    public T getItem() {
        return item;
    }

    public int getPosition() {
        return position;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public boolean hasPendingWrites() {
        return hasPendingWrites;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ELColStoreItemAddedEvent<?> that = (ELColStoreItemAddedEvent<?>) o;

        if (position != that.position) return false;
        if (hasPendingWrites != that.hasPendingWrites) return false;
        if (fromCache != that.fromCache) return false;
        return item != null ? item.equals(that.item) : that.item == null;
    }

    @Override
    public int hashCode() {
        int result = position;
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (hasPendingWrites ? 1 : 0);
        result = 31 * result + (fromCache ? 1 : 0);
        return result;
    }
}
