package com.everlog.ui.views.summarycard;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.everlog.R;
import com.everlog.databinding.ViewSummaryCardWeekBinding;

import androidx.annotation.Nullable;

public class SummaryCardWeek extends LinearLayout {

    private ViewSummaryCardWeekBinding binding;

    public SummaryCardWeek(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SummaryCardWeek(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SummaryCardWeek(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        setupLayout(context);
    }

    public void setIconPadding(int padding) {
        binding.iconImg.setPadding(padding, padding, padding, padding);
    }

    public void setSummary(int iconResId,
                           String title,
                           String metric,
                           String subtitle) {
        binding.iconImg.setImageResource(iconResId);
        binding.titleLbl.setText(title);
        binding.subtitleLbl.setText(subtitle);
        if (TextUtils.isEmpty(metric)) {
            binding.metricLbl.setVisibility(GONE);
        } else {
            binding.metricLbl.setVisibility(VISIBLE);
            binding.metricLbl.setText(metric);
        }
    }

    // Setup

    private void setupLayout(Context context) {
        binding = ViewSummaryCardWeekBinding.inflate(LayoutInflater.from(context), this, true);
    }
}
