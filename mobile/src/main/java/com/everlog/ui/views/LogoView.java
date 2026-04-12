package com.everlog.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

import com.everlog.R;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

public class LogoView extends AppCompatTextView {

    public LogoView(Context context) {
        super(context);
        init(null, 0);
    }

    public LogoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LogoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "UnderAuthority.ttf");
        setTypeface(tf);
        setText(R.string.everlog_app_name);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white_base));
        setGravity(Gravity.CENTER);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.LogoView, defStyleAttr, 0);

            try {
                boolean hasShadow = typedArray.getBoolean(R.styleable.LogoView_lvShadow, true);
                if (hasShadow) {
                    setShadowLayer(8.0f, 1f, 1f, Color.BLACK);
                }
            } finally {
                typedArray.recycle();
            }
        }
    }
}
