package com.everlog.utils.input;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtils {

    /* 128dp = 32dp * 4, minimum button height 32dp and generic 4 rows soft keyboard */
    private static final int SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD = 128;

    public static void hideKeyboard(Activity c) {
        try {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(c.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {}
    }

    public static void toggleKeyboard(Context c, EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static void showKeyboard(Context c, EditText editText) {
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(Context c, EditText editText) {
        try {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        } catch (Exception e) {}
    }

    public static boolean isKeyboardShown(View rootView, Rect rect) {
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        /* Threshold size: dp to pixels, multiply with display density */
        return keyboardHeight(rootView, rect) > SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD * dm.density;
    }

    public static int keyboardHeight(View rootView, Rect rect) {
        rootView.getWindowVisibleDisplayFrame(rect);
        return rootView.getBottom() - rect.bottom;
    }
}
