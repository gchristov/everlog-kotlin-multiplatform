package com.everlog.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.everlog.R;
import com.everlog.data.model.WeekDay;
import com.everlog.databinding.ViewWeekDayBinding;

import org.threeten.bp.LocalDateTime;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.widget.LinearLayout;

public class WeekDayView extends LinearLayout {

    private ViewWeekDayBinding binding;

    public WeekDayView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WeekDayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WeekDayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        setupLayout(context);
    }

    public void setWeekDay(WeekDay day) {
        binding.dateLbl.setText(day.getDate().getDayOfMonth() + "");
        binding.nameLbl.setText(day.getDay());
        int today = LocalDateTime.now().getDayOfMonth();
        if (day.getActive()) {
            // Active
            binding.dateLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.background_card));
            binding.nameLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.background_card));
            binding.containerLayout.setBackgroundResource(R.drawable.rounded_corners_week_day);
        } else if (today == day.getDate().getDayOfMonth()) {
            // Today
            binding.nameLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.white_base));
            binding.dateLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.white_base));
            binding.containerLayout.setBackground(null);
        } else {
            // Inactive
            binding.dateLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_5));
            binding.nameLbl.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_5));
            binding.containerLayout.setBackground(null);
        }
    }

    // Setup

    private void setupLayout(Context context) {
        binding = ViewWeekDayBinding.inflate(LayoutInflater.from(context), this, true);
    }
}
