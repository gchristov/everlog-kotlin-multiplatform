package com.everlog.ui.views.summarycard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.everlog.databinding.ViewSummaryCardCollapsingBinding;

import androidx.annotation.Nullable;

public class SummaryCardCollapsing extends LinearLayout {

    private ViewSummaryCardCollapsingBinding binding;

    public SummaryCardCollapsing(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SummaryCardCollapsing(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SummaryCardCollapsing(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        setupLayout(context);
    }

    public void setSummary(String title,
                           String subtitle) {
        binding.titleLbl.setText(title);
        binding.subtitleLbl.setText(subtitle);
    }

    // Setup

    private void setupLayout(Context context) {
        binding = ViewSummaryCardCollapsingBinding.inflate(LayoutInflater.from(context), this, true);
    }
}
