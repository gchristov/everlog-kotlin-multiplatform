package com.everlog.ui.views.apprate;

import com.everlog.managers.analytics.AnalyticsManager;
import com.everlog.managers.apprate.AppLaunchManager;
import com.everlog.ui.navigator.Navigator;
import com.everlog.ui.views.base.BaseViewPresenter;
import com.everlog.utils.Utils;

import java.util.concurrent.TimeUnit;

public class PresenterAppRate extends BaseViewPresenter<MvpViewAppRate> {

    @Override
    public void onReady() {
        observeRatingChanged();
        observeNotNowClick();
        observeFeedbackClick();
    }

    // Observers

    private void observeRatingChanged() {
        subscriptions.add(getMvpView().onRatingChanged()
                .debounce(600, TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe(rating -> {
                    AnalyticsManager.manager.appStarRating(rating);
                    handleRatingChanged(rating);
                }, throwable -> handleError(throwable)));
    }

    private void observeNotNowClick() {
        subscriptions.add(getMvpView().onClickNotNow()
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    AnalyticsManager.manager.appNotNow();
                    AppLaunchManager.manager.rateDialogDismissed();
                    handleHideRating(false);
                }, throwable -> handleError(throwable)));
    }

    private void observeFeedbackClick() {
        subscriptions.add(getMvpView().onClickFeedback()
                .compose(applyUISchedulers())
                .subscribe(action -> {
                    AnalyticsManager.manager.appFeedback();
                    AppLaunchManager.manager.rateDialogDismissed();
                    navigator.sendEmail(Navigator.ContactType.FEEDBACK, null);
                    handleHideRating(false);
                }, throwable -> handleError(throwable)));
    }

    // Handlers

    private void handleHideRating(boolean wait) {
        Utils.runWithDelay(() -> {
            if (isAttachedToView()) {
                getMvpView().hideRating();
            }
        }, wait ? 500 : 0);
    }

    private void handleRatingChanged(float rating) {
        if (rating < 4) {
            getMvpView().showFeedbackPrompt();
        } else {
            // Open Play Store directly
            AnalyticsManager.manager.appRate();
            AppLaunchManager.manager.rateDialogDismissed();
            navigator.openPlayStoreAppDetails();
            handleHideRating(true);
        }
    }
}
