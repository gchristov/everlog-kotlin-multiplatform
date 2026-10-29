package com.everlog.data.datastores.events.collection;

import com.everlog.data.datastores.events.BaseEvent;

public class ELColStoreItemModifiedEvent<T> extends BaseEvent {

    private int oldPosition;
    private int newPosition;
    private T item;
    private boolean hasPendingWrites;
    private boolean fromCache;

    public ELColStoreItemModifiedEvent(int oldPosition,
                                       int newPosition,
                                       T item,
                                       boolean hasPendingWrites,
                                       boolean fromCache) {
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.item = item;
        this.hasPendingWrites = hasPendingWrites;
        this.fromCache = fromCache;
    }

    public T getItem() {
        return item;
    }

    public int getOldPosition() {
        return oldPosition;
    }

    public int getNewPosition() {
        return newPosition;
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

        ELColStoreItemModifiedEvent<?> that = (ELColStoreItemModifiedEvent<?>) o;

        if (oldPosition != that.oldPosition) return false;
        if (newPosition != that.newPosition) return false;
        if (hasPendingWrites != that.hasPendingWrites) return false;
        if (fromCache != that.fromCache) return false;
        return item != null ? item.equals(that.item) : that.item == null;
    }

    @Override
    public int hashCode() {
        int result = oldPosition;
        result = 31 * result + newPosition;
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (hasPendingWrites ? 1 : 0);
        result = 31 * result + (fromCache ? 1 : 0);
        return result;
    }
}
