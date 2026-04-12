package com.everlog.ui.views.recyclerview.touch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

public class TouchDisаbledRecyclerView extends RecyclerView {

    public TouchDisаbledRecyclerView(Context context) {
        super(context);
    }

    public TouchDisаbledRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchDisаbledRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }
}