package com.everlog.ui.fragments.home.week.views;

import android.view.View;

import com.everlog.ui.activities.base.BaseActivity;

public interface IWeekView {

    void onCreateView(View view);

    void toggleVisible(boolean show);

    void toggleLoading(boolean show, BaseActivity parent);

    String getTitle();
}
