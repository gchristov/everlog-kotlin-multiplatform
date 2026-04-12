package com.everlog.ui.views.apprate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.everlog.R;
import com.everlog.databinding.ViewAppRateBinding;
import com.everlog.managers.apprate.AppLaunchManager;
import com.everlog.ui.views.base.BaseView;
import com.everlog.ui.views.base.BaseViewPresenter;
import com.everlog.utils.text.TextViewUtils;
import com.everlog.utils.text.TouchableSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AppRateView extends BaseView implements MvpViewAppRate {

    private ViewAppRateBinding binding;

    private PresenterAppRate mPresenter;

    private PublishSubject<Void> mOnClickNotNow = PublishSubject.create();
    private PublishSubject<Void> mOnClickFeedback = PublishSubject.create();
    private PublishSubject<Float> mOnRatingChanged = PublishSubject.create();

    public AppRateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setupLayout(AttributeSet attrs, int defStyleAttr) {
        binding = ViewAppRateBinding.inflate(LayoutInflater.from(getContext()), this, true);
    }

    @Override
    public void onViewCreated() {
        setupTextViews();
        setupStarView();
        setupClickListeners();
        showStarPrompt();
    }

    @Override
    public int getLayoutResId() {
        return 0;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BaseViewPresenter getPresenter() {
        return mPresenter;
    }

    private void setupClickListeners() {
        binding.secondPromptNoBtn.setOnClickListener(this::onClickSecondNo);
        binding.secondPromptYesBtn.setOnClickListener(this::onClickSecondYes);
    }

    public void onClickSecondNo(View view) {
        mOnClickNotNow.onNext(null);
    }

    public void onClickSecondYes(View view) {
        mOnClickFeedback.onNext(null);
    }

    @Override
    public Observable<Void> onClickNotNow() {
        return mOnClickNotNow;
    }

    @Override
    public Observable<Void> onClickFeedback() {
        return mOnClickFeedback;
    }

    @Override
    public Observable<Float> onRatingChanged() {
        return mOnRatingChanged;
    }

    @Override
    public void hideRating() {
        setVisibility(GONE);
    }

    @Override
    public void showFeedbackPrompt() {
        binding.actionLbl.setText(R.string.rate_feedback_title);
        binding.firstPrompt.setVisibility(View.GONE);
        binding.secondPrompt.setVisibility(View.VISIBLE);
    }

    public void checkAppRate() {
        boolean shouldRate = AppLaunchManager.manager.shouldShowAppRating();
        if (getVisibility() != VISIBLE) {
            setVisibility(shouldRate ? VISIBLE : GONE);
        }
    }

    private void showStarPrompt() {
        binding.actionLbl.setText(R.string.rate_enjoy_title);
        binding.firstPrompt.setVisibility(View.VISIBLE);
        binding.secondPrompt.setVisibility(View.GONE);
    }

    // Setup

    @Override
    public void setupPresenter() {
        mPresenter = new PresenterAppRate();
    }

    private void setupTextViews() {
        // Initialize spans
        TouchableSpan askLaterSpan = new TouchableSpan(binding.askLaterBtn.getCurrentTextColor()) {

            @Override
            public void onClick(View widget) {
                mOnClickNotNow.onNext(null);
            }
        };
        TouchableSpan[] spans = new TouchableSpan[]{askLaterSpan};

        // Initialize clickable text
        String[] clickableTexts = new String[]{
                getContext().getString(R.string.rate_ask_later),
        };

        TextViewUtils.addClickableSpans(binding.askLaterBtn, spans, getContext().getString(R.string.rate_ask_later), clickableTexts);
    }

    private void setupStarView() {
        binding.starView.setOnRatingChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                mOnRatingChanged.onNext(rating);
            }
        });
    }
}
