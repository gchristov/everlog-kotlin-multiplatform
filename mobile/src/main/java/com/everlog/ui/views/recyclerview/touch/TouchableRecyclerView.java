package com.everlog.ui.views.recyclerview.touch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

public class TouchableRecyclerView extends RecyclerView {

    private boolean mAllowChildClick = true;
    private OnRecyclerViewClickListener mClickListener;

    public TouchableRecyclerView(@NonNull Context context) {
        super(context);
    }

    public TouchableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnRecyclerClickListener(OnRecyclerViewClickListener listener) {
        setOnRecyclerClickListener(listener, true);
    }

    public void setOnRecyclerClickListener(OnRecyclerViewClickListener listener, boolean includeClickOnItems) {
        this.mAllowChildClick = includeClickOnItems;
        this.mClickListener = listener;
    }

    private float mPrevMotionY, mPrevMotionX;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // The findChildViewUnder() method returns null if the touch event
        // occurs outside of a child View.
        // Change the MotionEvent action as needed. Here we use ACTION_DOWN
        // as a simple, naive indication of a click.
        final int action = MotionEventCompat.getActionMasked(ev);
        final float y = ev.getY();
        final float x = ev.getX();
        float dy, dx;
        final int dragSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        if (action == MotionEvent.ACTION_DOWN) {
            mPrevMotionY = y;
            mPrevMotionX = x;
        } else if (action == MotionEvent.ACTION_UP) {
            dy = y - mPrevMotionY;
            mPrevMotionY = y;

            dx = x - mPrevMotionX;
            mPrevMotionX = x;
            if (Math.abs(dy) < dragSlop
                    && Math.abs(dx) < dragSlop
                    && (mAllowChildClick || findChildViewUnder(x, y) == null)) {
                if (mClickListener != null) {
                    mClickListener.onClick();
                }
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    public interface OnRecyclerViewClickListener {

        void onClick();
    }
}
