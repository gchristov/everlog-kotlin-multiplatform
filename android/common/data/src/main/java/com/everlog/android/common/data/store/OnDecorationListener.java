package com.everlog.android.common.data.store;

public interface OnDecorationListener<T> {

    void onDecorationComplete(T item);

    void onError(Throwable throwable);
}
