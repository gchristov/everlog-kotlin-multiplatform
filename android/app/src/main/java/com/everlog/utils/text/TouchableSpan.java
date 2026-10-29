package com.everlog.utils.text;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;

public abstract class TouchableSpan extends ClickableSpan {

    private static final int DEFAULT_ALPHA = 255;
    private static final int PRESSED_ALPHA = 100;

    private boolean mIsPressed;
    private int textColor;

    public TouchableSpan(int textColor) {
        this.textColor = textColor;
    }

    public void setPressed(boolean isSelected) {
        mIsPressed = isSelected;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(textColor);
        ds.bgColor = Color.TRANSPARENT;
        ds.setAlpha(mIsPressed ? PRESSED_ALPHA : DEFAULT_ALPHA);
        ds.setUnderlineText(true);
    }
}
