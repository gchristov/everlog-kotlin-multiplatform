package com.everlog.data.datastores.base;

/**
 * Fills in what an item references by ID, e.g. a routine's exercises.
 */
public interface ItemDecorator<T> {

    void decorate(T item);
}
