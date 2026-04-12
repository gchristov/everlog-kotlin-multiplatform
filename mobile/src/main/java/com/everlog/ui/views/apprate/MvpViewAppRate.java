package com.everlog.ui.views.apprate;

import com.everlog.ui.views.base.BaseViewMvpView;

import rx.Observable;

public interface MvpViewAppRate extends BaseViewMvpView {

    Observable<Void> onClickNotNow();

    Observable<Void> onClickFeedback();

    Observable<Float> onRatingChanged();

    void showFeedbackPrompt();

    void hideRating();
}
