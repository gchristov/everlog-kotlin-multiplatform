package com.everlog.data.datastores.base;

public interface OnDecorationListener<T> {

    void onDecorationComplete(T item);

    void onError(Throwable throwable);
}
