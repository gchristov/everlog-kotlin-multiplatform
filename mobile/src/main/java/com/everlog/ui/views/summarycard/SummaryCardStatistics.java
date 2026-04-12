package com.everlog.ui.views.summarycard;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.everlog.R;

import androidx.annotation.Nullable;

public class SummaryCardStatistics extends LinearLayout {

    private TextView mTitleLbl;
    private TextView mMetricLbl;
    private TextView mSubtitleLbl;

    public SummaryCardStatistics(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SummaryCardStatistics(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SummaryCardStatistics(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    public void setSummary(String title,
                           String metric,
                           String subtitle) {
        mTitleLbl.setText(title);
        mSubtitleLbl.setText(subtitle);
        if (TextUtils.isEmpty(metric)) {
            mMetricLbl.setVisibility(GONE);
        } else {
            mMetricLbl.setVisibility(VISIBLE);
            mMetricLbl.setText(metric);
        }
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        int layoutId = R.layout.view_summary_card_statistics;
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SummaryCardStatistics, defStyleAttr, 0);

            try {
                layoutId = typedArray.getResourceId(R.styleable.SummaryCardStatistics_scsLayout, layoutId);
            } finally {
                typedArray.recycle();
            }
        }
        View view = View.inflate(getContext(), layoutId, this);
        mTitleLbl = view.findViewById(R.id.titleLbl);
        mMetricLbl = view.findViewById(R.id.metricLbl);
        mSubtitleLbl = view.findViewById(R.id.subtitleLbl);
    }
}
