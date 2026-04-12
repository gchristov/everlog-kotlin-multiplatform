package com.everlog.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.everlog.R;
import com.everlog.data.model.pro.ELProSkuDetails;
import com.everlog.databinding.ViewBtnProSubBinding;

import androidx.annotation.Nullable;
import rx.Observable;
import rx.subjects.PublishSubject;

public class ProSubButton extends LinearLayout {

    private ViewBtnProSubBinding binding;

    private ELProSkuDetails mDetails;

    private PublishSubject<ELProSkuDetails> mOnClick = PublishSubject.create();

    public ProSubButton(Context context) {
        super(context);
        init(null, 0);
    }

    public ProSubButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ProSubButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
        binding.containerLayout.setOnClickListener(v -> {
            if (mDetails != null) {
                mOnClick.onNext(mDetails);
            }
        });
    }

    public Observable<ELProSkuDetails> getOnClickListener() {
        return mOnClick;
    }

    public void toggleLoading(boolean show) {
        binding.progressBar.setVisibility(show ? VISIBLE : GONE);
        binding.contentPanel.setVisibility(GONE);
        binding.errorView.setVisibility(GONE);
    }

    public void showError() {
        binding.progressBar.setVisibility(GONE);
        binding.contentPanel.setVisibility(GONE);
        binding.errorView.setVisibility(VISIBLE);
    }

    public void showSku(ELProSkuDetails details) {
        this.mDetails = details;
        binding.progressBar.setVisibility(GONE);
        binding.contentPanel.setVisibility(VISIBLE);
        binding.errorView.setVisibility(GONE);
        renderSku();
    }

    // Render

    private void renderSku() {
        binding.titleLbl.setText(mDetails.weeklyPriceSummary());
        binding.subtitleLbl.setText(mDetails.priceSummary(getContext()));
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        binding = ViewBtnProSubBinding.inflate(LayoutInflater.from(getContext()), this, true);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ProSubButton, defStyleAttr, 0);

            try {
                boolean hasBackground = typedArray.hasValue(R.styleable.ProSubButton_psbBackground);
                if (hasBackground) {
                    binding.containerLayout.setBackground(typedArray.getDrawable(R.styleable.ProSubButton_psbBackground));
                }
            } finally {
                typedArray.recycle();
            }
        }
    }
}
