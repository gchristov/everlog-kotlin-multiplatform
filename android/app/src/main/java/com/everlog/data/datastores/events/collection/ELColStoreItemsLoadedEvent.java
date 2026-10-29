package com.everlog.data.datastores.events.collection;

import com.everlog.data.datastores.events.BaseEvent;

import java.util.List;

public class ELColStoreItemsLoadedEvent<T> extends BaseEvent {

    private List<T> items;
    private Throwable error;
    private boolean fromCache;

    public ELColStoreItemsLoadedEvent(List<T> items,
                                      Throwable error,
                                      boolean fromCache) {
        this.items = items;
        this.error = error;
        this.fromCache = fromCache;
    }

    public Throwable getError() {
        return error;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ELColStoreItemsLoadedEvent<?> that = (ELColStoreItemsLoadedEvent<?>) o;

        if (fromCache != that.fromCache) return false;
        if (items != null ? !items.equals(that.items) : that.items != null) return false;
        return error != null ? error.equals(that.error) : that.error == null;
    }

    @Override
    public int hashCode() {
        int result = items != null ? items.hashCode() : 0;
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (fromCache ? 1 : 0);
        return result;
    }
}
