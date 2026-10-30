package com.everlog.android.common.mvp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.everlog.android.common.kotlin.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

// import icepick.Icepick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public abstract class BasePresenter<T extends BaseMvpView> {

    private static final String TAG = "BasePresenter";

    protected CompositeSubscription subscriptions;
    private Context context;

    private T mvpView;

    public abstract void onReady();

    public BasePresenter() {
        // No-op.
    }

    public void init() {
        // No-op.
    }

    public void onRestoreInstanceState(Bundle inState) {
        // Icepick.restoreInstanceState(this, inState);
    }

    public void onSaveInstanceState(Bundle outState) {
        // Icepick.saveInstanceState(this, outState);
    }

    /**
     * The context of the view last attached. Unlike the view, it stays after detaching, so work finishing late
     * can still open a screen.
     */
    public @Nullable Context getContext() {
        return context;
    }

    public T getMvpView() {
        return mvpView;
    }

    public void attachView(T mvpView) {
        this.mvpView = mvpView;
        this.subscriptions = new CompositeSubscription();
        this.context = mvpView.getContext();
        // Register to receive events.
        EventBus.getDefault().register(this);
    }

    public void detachView() {
        // Unregister from receiving events.
        EventBus.getDefault().unregister(this);
        mvpView = null;
        if (subscriptions != null && subscriptions.hasSubscriptions()) {
            subscriptions.unsubscribe();
            subscriptions.clear();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void placeholder(BaseEvent event) {
        // No-op
    }

    protected boolean isAttachedToView() {
        return mvpView != null;
    }

    protected void sendBroadcast(Intent intent) {
        if (isAttachedToView() && Utils.isValidContext(getMvpView().getContext())) {
            LocalBroadcastManager.getInstance(getMvpView().getContext()).sendBroadcast(intent);
        }
    }

    // Schedulers

    protected <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    protected <T> Observable.Transformer<T, T> applyIOSchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    protected <T> Observable.Transformer<T, T> applyUISchedulers() {
        return observable -> observable.subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Errors

    protected void handleError(Throwable throwable) {
        throwable.printStackTrace();
        Timber.tag(TAG).e(throwable);
    }
}
