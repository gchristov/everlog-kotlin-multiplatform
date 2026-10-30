package com.everlog.ui.fragments.home.week.views;

import android.view.View;

import com.everlog.android.common.mvp.activity.BaseActivity;
import com.everlog.ui.fragments.home.week.WeekViewState;

public interface IWeekView {

    void onCreateView(View view);

    void render(WeekViewState state, BaseActivity parent);

    String getTitle();
}
