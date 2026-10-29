package com.everlog.ui.mvp;

import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.everlog.data.datastores.events.BaseEvent;
import com.everlog.data.model.ELUser;
import com.everlog.managers.auth.LocalUserManager;
import com.everlog.ui.navigator.ELNavigator;
import com.everlog.ui.navigator.Navigator;
import com.everlog.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.annotation.Nullable;

// import icepick.Icepick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public abstract class BasePresenter<T extends BaseMvpView> {

    private static final String TAG = "BasePresenter";

    protected CompositeSubscription subscriptions;
    protected Navigator navigator;

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

    public T getMvpView() {
        return mvpView;
    }

    public void attachView(T mvpView) {
        this.mvpView = mvpView;
        this.subscriptions = new CompositeSubscription();
        this.navigator = new ELNavigator(getMvpView().getContext());
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

    // User account

    protected @Nullable ELUser getUserAccount() {
        return LocalUserManager.getUser();
    }
}
