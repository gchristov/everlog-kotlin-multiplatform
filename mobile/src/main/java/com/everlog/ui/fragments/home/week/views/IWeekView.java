package com.everlog.ui.fragments.home.week.views;

import android.view.View;

import com.everlog.ui.activities.base.BaseActivity;
import com.everlog.ui.fragments.home.week.WeekViewState;

public interface IWeekView {

    void onCreateView(View view);

    void render(WeekViewState state, BaseActivity parent);

    String getTitle();
}
